package ru.studyground

import ru.studyground.jwt.JwtToken
import zhttp.http._
import zio.blocking.Blocking
import zio.json._
import zio.stream.ZStream
import zio.{Has, URIO, ZIO}

import java.nio.file.{Files, Paths}

package object http {

  private[http] def fromAsset(
      path: Path
  ): URIO[Has[AppConfig] with Blocking, Response[Blocking, HttpError]] =
    ZIO.access[Has[AppConfig]](hasConf =>
      readResource(path, hasConf.get.webDirs).fold(
        Response.fromHttpError,
        s =>
          Response.http(
            content = HttpData.fromStream(s)
          )
      )
    )

  private def readResource(
      path: Path,
      assetPaths: List[String]
  ): Either[HttpError, ZStream[Blocking, HttpError, Byte]] = {
    val p = path.toList.mkString("/")
    val route = assetPaths.find(dir => Files.exists(Paths.get(s"$dir/$p")))
    route.fold[Either[HttpError, ZStream[Blocking, HttpError, Byte]]](
      Left(HttpError.NotFound(path))
    )(dir =>
      Right(
        ZStream
          .fromFile(Paths.get(s"$dir/$p"))
          .orElseFail(HttpError.InternalServerError())
      )
    )
  }

  def extractData[A](
      r: Request
  )(implicit A: JsonDecoder[A]): ZIO[Any, HttpError, A] =
    for {
      mbData <-
        ZIO.fromOption(r.getBodyAsString).orElseFail(HttpError.BadRequest())
      data <-
        ZIO
          .fromEither(mbData.fromJson[A])
          .mapError(HttpError.BadRequest)
    } yield data

  def authorized[R](
      pf: PartialFunction[Request, Response[R, HttpError]]
  ): HttpApp[R with Has[JwtToken], HttpError] = tokenAuthorization.auth(pf)

  def authorizedM[R](
      pf: PartialFunction[Request, ZIO[R, HttpError, Response[R, HttpError]]]
  ): HttpApp[R with Has[JwtToken], HttpError] = tokenAuthorization.authM(pf)

}
