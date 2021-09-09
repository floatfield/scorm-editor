package ru.studyground.domain.user

import io.getquill.context.ZioJdbc._
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import io.getquill.{NamingStrategy, PostgresZioJdbcContext, SnakeCase}
import zio.{Has, IO, Task, ZIO, ZLayer}
import ru.studyground.PasswordEncryption

import java.io.Closeable
import java.sql.SQLException
import javax.sql.DataSource

case class UserDTO(
    id: Long,
    name: String,
    password: Array[Byte],
    salt: Array[Byte]
)

object UserDBRepo {
  type Env = Has[DataSource with Closeable] with Has[PasswordEncryption]

  val live: ZLayer[Env, Nothing, Has[UserRepoService]] = ZLayer
    .fromServices[
      DataSource with Closeable,
      PasswordEncryption,
      UserRepoService
    ](
      UserDBServiceService
    )

}

case class UserDBServiceService(
    ds: DataSource with Closeable,
    encryptor: PasswordEncryption
) extends UserRepoService {

  object Ctx extends PostgresZioJdbcContext(NamingStrategy(SnakeCase))
  import Ctx._

  implicit val userInsertMeta =
    insertMeta[UserDTO](_.id)

  val users = quote {
    querySchema[UserDTO]("users")
  }

  implicit val env: Implicit[Has[DataSource with Closeable]] = Implicit(Has(ds))

  override def create(name: String, password: String): Task[Unit] = {
    for {
      _ <-
        ZIO
          .fail(UserExistsException(name))
          .whenM(getUser(name).map(_.isDefined))
      h <- encryptor.hash(password)
      _ <-
        Ctx
          .run(
            quote(
              users.insert(
                lift(UserDTO(-1L, name, h.hash, h.salt))
              )
            )
          )
          .implicitDS

    } yield ()
  }

  override def get(name: String): Task[Option[User]] =
    getUser(name).map(
      _.map(userDTO => User(userDTO.name, userDTO.password))
    )

  override def delete(name: String): Task[Unit] =
    Ctx.run(deleteUserByName(name)).unit.implicitDS

  override def isValid(name: String, password: String): Task[Boolean] =
    getUser(name).flatMap {
      case Some(UserDTO(_, _, p, s)) =>
        encryptor.hash(password, s).map(_.sameElements(p))
      case None => ZIO.succeed(false)
    }

  private def deleteUserByName(name: String) =
    quote(
      users.filter(_.name == lift(name)).delete
    )

  private def getUser(name: String): IO[SQLException, Option[UserDTO]] =
    Ctx
      .run(quote(users.filter(_.name == lift(name))))
      .map(_.headOption)
      .implicitDS
}
