package ru.studyground.jwt

import pdi.jwt.JwtAlgorithm.HS512
import pdi.jwt.{JwtClaim, JwtHeader}
import ru.studyground.config.AppConfig
import zio.json._
import zio.{Has, IO, UIO, URIO, URLayer, ZIO, ZLayer}

case class InvalidToken(token: String) extends Exception

case class UserToken(name: String)

object UserToken {
  implicit val encoder: JsonEncoder[UserToken] =
    DeriveJsonEncoder.gen[UserToken]
  implicit val decoder: JsonDecoder[UserToken] =
    DeriveJsonDecoder.gen[UserToken]
}

trait JwtToken {
  def encode[A](tokenData: A)(implicit A: JsonEncoder[A]): UIO[String]
  def decode[A](token: String)(implicit A: JsonDecoder[A]): UIO[Option[A]]
  def validate[A](token: String)(implicit
      A: JsonDecoder[A]
  ): IO[InvalidToken, Unit]
}

object JwtToken {
  def encode[A](
      tokenData: A
  )(implicit A: JsonEncoder[A]): URIO[Has[JwtToken], String] =
    ZIO.accessM(_.get.encode(tokenData))

  def decode[A](
      token: String
  )(implicit A: JsonDecoder[A]): URIO[Has[JwtToken], Option[A]] =
    ZIO.accessM(_.get.decode(token))

  def validate[A](
      token: String
  )(implicit A: JsonDecoder[A]): ZIO[Has[JwtToken], InvalidToken, Unit] =
    ZIO.accessM(_.get.validate(token))

  private val algorithm = HS512

  val live: URLayer[Has[AppConfig], Has[JwtToken]] = ZLayer.fromService[AppConfig, JwtToken](config =>
    new JwtToken {
      override def encode[A](tokenData: A)(implicit
          A: JsonEncoder[A]
      ): UIO[String] =
        ZIO.effectTotal {
          val claim =
            JwtClaim(content = tokenData.toJson)
          val header = JwtHeader(algorithm)
          JwtZIO.encode(header, claim, config.secretKey)
        }

      override def decode[A](token: String)(implicit
          A: JsonDecoder[A]
      ): UIO[Option[A]] =
        ZIO.effectTotal(
          JwtZIO
            .decode(token, config.secretKey, List(algorithm))
            .toOption
            .flatMap(claim => claim.content.fromJson[A].toOption)
        )

      override def validate[A](token: String)(implicit
          A: JsonDecoder[A]
      ): IO[InvalidToken, Unit] =
        ZIO
          .fromTry(
            JwtZIO
              .decode(token, config.secretKey, List(algorithm))
          )
          .bimap(_ => InvalidToken(token), _ => ())
    }
  )
}
