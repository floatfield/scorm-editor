package ru
package studyground

import zio.clock.Clock
import zio.random.Random
import zio.{Has, RIO, Task, ZIO, ZLayer}

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait PasswordEncryption {
  def hash(s: String): Task[SaltyHash]
  def hash(s: String, salt: Array[Byte]): Task[Array[Byte]]
}

final case class SaltyHash(hash: Array[Byte], salt: Array[Byte])

object PasswordEncryption {

  def hash(s: String): RIO[Has[PasswordEncryption], SaltyHash] =
    ZIO.accessM(_.get.hash(s))

  val live: ZLayer[Random, Nothing, Has[PasswordEncryption]] =
    ZLayer.fromService[Random.Service, PasswordEncryption](PasswordEncryptor)

}

final case class PasswordEncryptor(random: Random.Service)
    extends PasswordEncryption {

  override def hash(s: String): Task[SaltyHash] = {
    for {
      salt <- random.nextBytes(20).map(_.toArray)
      pass <- hash(s, salt)
    } yield SaltyHash(pass, salt)
  }

  override def hash(s: String, salt: Array[Byte]): Task[Array[Byte]] =
    for {
      spec <- ZIO.effect(new PBEKeySpec(s.toCharArray, salt, 65536, 128))
      factory <- ZIO.effect(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"))
      pass <- ZIO.effect(factory.generateSecret(spec).getEncoded)
    } yield pass
}
