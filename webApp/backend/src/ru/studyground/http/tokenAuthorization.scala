package ru.studyground.http

import ru.studyground.jwt.{JwtToken, UserToken}
import zhttp.http._
import zio.{Has, ZIO}

object tokenAuthorization {
  type BucketsEnv = Has[JwtToken]

  private def validateToken(
      r: Request
  ): ZIO[Has[JwtToken], HttpError, Request] =
    for {
      token <-
        ZIO
          .fromOption(r.getBearerToken)
          .orElseFail(HttpError.Forbidden())
      _ <-
        JwtToken
          .validate(token)
          .mapError(err =>
            HttpError.BadRequest(s"token ${err.token} is invalid")
          )
    } yield r

  def auth[R](
      pf: PartialFunction[Request, Response[R, HttpError]]
  ): HttpApp[R with Has[JwtToken], HttpError] =
    Http.collectM[Request] {
      case r if pf.isDefinedAt(r) =>
        validateToken(r).collect(HttpError.InternalServerError())(pf)
    }

  def authM[R](
      pf: PartialFunction[Request, ZIO[R, HttpError, Response[R, HttpError]]]
  ): HttpApp[R with Has[JwtToken], HttpError] =
    Http.collectM[Request] {
      case r if pf.isDefinedAt(r) =>
        validateToken(r).collectM(HttpError.InternalServerError())(pf)
    }
}
