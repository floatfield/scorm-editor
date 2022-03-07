package ru.studyground.http

import ru.studyground.jwt.{JwtToken, UserToken}
import zhttp.http.HasCookie.RequestCookie
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
          .fromOption(RequestCookie.decode(r).find(_.name == "token"))
          .orElseFail(HttpError.Forbidden())
      _ <-
        JwtToken
          .validate(token.content)
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

  def authRedirectM[R](
      location: String,
      pf: PartialFunction[Request, ZIO[R, HttpError, Response[R, HttpError]]]
  ): HttpApp[R with Has[JwtToken], HttpError] =
    Http.collectM[Request] {
      case r if pf.isDefinedAt(r) =>
        validateToken(r).foldM(
          {
            case HttpError.BadRequest(_) => ZIO.succeed(Response.redirect(location))
            case e => ZIO.fail(e)
          },
          req => pf(req)
        )
    }
}
