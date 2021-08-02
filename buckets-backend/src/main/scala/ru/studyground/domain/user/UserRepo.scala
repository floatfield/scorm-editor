package ru
package studyground
package domain.user

import com.google.protobuf.ByteString
import zio.blocking.Blocking
import zio.console.Console
import zio.random.Random
import zio.{Has, RIO, Task, ZIO, ZLayer, ZManaged}

import java.nio.charset.StandardCharsets

case class UserExistsException(name: String) extends Exception

trait UserRepo {
  def create(name: String, password: String): RIO[Blocking, Unit]
  def get(name: String): RIO[Blocking, Option[User]]
  def delete(name: String): RIO[Blocking, Unit]
  def isValid(name: String, password: String): RIO[Blocking, Boolean]
}

object UserRepo {

  private type UserRepoEnv = Blocking with Has[UserRepo]

  def create(name: String, password: String): RIO[UserRepoEnv, Unit] =
    ZIO.accessM(_.get[UserRepo].create(name, password))

  def get(name: String): RIO[UserRepoEnv, Option[User]] =
    ZIO.accessM(_.get[UserRepo].get(name))

  def delete(name: String): RIO[UserRepoEnv, Unit] =
    ZIO.accessM(_.get[UserRepo].delete(name))

  def isValid(name: String, password: String): RIO[UserRepoEnv, Boolean] =
    ZIO.accessM(_.get[UserRepo].isValid(name, password))

  private implicit val stringRepoCodec: RepoCodec[String] =
    new RepoCodec[String] {
      override def toByteArray(a: String): Task[Array[Byte]] =
        ZIO.effect(a.getBytes(StandardCharsets.UTF_8))
      override def fromByteArray(bytes: Array[Byte]): Task[String] =
        ZIO.succeed(new String(bytes, StandardCharsets.UTF_8))
    }

  private implicit val userDTORepoCodec: RepoCodec[UserDTO] =
    new RepoCodec[UserDTO] {
      override def toByteArray(a: UserDTO): Task[Array[Byte]] =
        ZIO.succeed(a.toByteArray)
      override def fromByteArray(bytes: Array[Byte]): Task[UserDTO] =
        ZIO.effect(UserDTO.parseFrom(bytes))
    }

  private val userRepository
      : ZLayer[Blocking with Console with Has[AppConfig], Throwable, Has[Repository[String, UserDTO]]] =
    ZManaged.environment[Has[AppConfig]].flatMap(config =>
      Repository.mkRepository[String, UserDTO](config.get.userRepo)
    ).toLayer

  val live: ZLayer[Blocking with Random with Console with Has[AppConfig], Throwable, Has[UserRepo]] =
    (userRepository ++ PasswordEncryption.live) >>> ZLayer
      .fromServices[Repository[String, UserDTO], PasswordEncryption, UserRepo](
        UserRepository
      )

}

final case class UserRepository(
    repo: Repository[String, UserDTO],
    encryptor: PasswordEncryption
) extends UserRepo {

  override def create(name: String, password: String): RIO[Blocking, Unit] =
    for {
      _ <-
        ZIO
          .fail(UserExistsException(name))
          .whenM(repo.get(name).map(_.isDefined))
      hash <- encryptor.hash(password)
      _ <- repo.put(name, mkUserDTO(name, hash.hash, hash.salt))
    } yield ()

  private def mkUserDTO(
      name: String,
      password: Array[Byte],
      salt: Array[Byte]
  ): UserDTO =
    UserDTO(
      name = name,
      password = ByteString.copyFrom(password),
      salt = ByteString.copyFrom(salt)
    )

  override def get(name: String): RIO[Blocking, Option[User]] =
    repo.get(name).map(_.map(u => User(u.name, u.password.toByteArray)))

  override def delete(name: String): RIO[Blocking, Unit] =
    repo.delete(name)

  override def isValid(name: String, password: String): RIO[Blocking, Boolean] =
    repo.get(name).flatMap {
      case None => ZIO.succeed(false)
      case Some(UserDTO(_, pass, salt, _)) =>
        encryptor.hash(password, salt.toByteArray).map(_.sameElements(pass.toByteArray))
    }
}
