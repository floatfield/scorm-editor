package ru.studyground

import ru.studyground.jwt.JwtToken
import zhttp.http._
import zio.blocking.{Blocking, effectBlocking}
import zio.json._
import zio.stream.ZStream
import zio.{Has, URIO, ZIO}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

package object http {

  type PartialReq[R] = PartialFunction[Request, ZIO[R, HttpError, Response[R, HttpError]]]

  private[http] def fromAsset(
      path: String
  ): URIO[Blocking, Response[Blocking, HttpError]] =
    readResource(path).fold(
      Response.fromHttpError,
      s => Response.http(data = HttpData.fromStream(s))
    )

  private[http] def readResource(
      path: String
  ): ZIO[Blocking, HttpError, ZStream[Blocking, HttpError, Byte]] =
    effectBlocking[Boolean](
      Files.exists(Paths.get(path))
    ).filterOrFail(identity)(HttpError.NotFound(Path(path)))
      .bimap(
        _ => HttpError.InternalServerError(),
        { _ =>
          ZStream
            .fromFile(Paths.get(path))
            .orElseFail(HttpError.InternalServerError())
        }
      )

  def extractData[A](
      r: Request
  )(implicit A: JsonDecoder[A]): ZIO[Any, HttpError, A] =
    for {
      mbData <- r.getBodyAsString.orElseFail(HttpError.BadRequest())
      data <-
        ZIO
          .fromEither(mbData.fromJson[A])
          .mapError(HttpError.BadRequest)
    } yield data

  def authorized[R](
      pf: PartialFunction[Request, Response[R, HttpError]]
  ): HttpApp[R with Has[JwtToken], HttpError] = tokenAuthorization.auth(pf)

  def authorizedM[R](
      pf: PartialReq[R]
  ): HttpApp[R with Has[JwtToken], HttpError] = tokenAuthorization.authM(pf)

  def jsonContent[R]: Http[R, HttpError, Request, Request] = Http.collect {
    case r if r.isJsonContentType => r
  }

}
