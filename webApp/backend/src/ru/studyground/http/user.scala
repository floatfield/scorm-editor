package ru
package studyground
package http

import ru.studyground.config.AppConfig
import ru.studyground.domain.user.UserRepoService
import ru.studyground.jwt.{JwtToken, UserToken}
import zhttp.http.Header.authorization
import zhttp.http._
import zio.blocking.Blocking
import zio.json._
import zio.{Chunk, Has, ZIO}

case class LoginRequest(login: String, password: String)

object LoginRequest {
  implicit val encoder: JsonEncoder[LoginRequest] =
    DeriveJsonEncoder.gen[LoginRequest]
  implicit val decoder: JsonDecoder[LoginRequest] =
    DeriveJsonDecoder.gen[LoginRequest]
}

object user {

  type UserEnv =
    Has[UserRepoService] with Has[JwtToken] with Has[AppConfig] with Blocking

  val userRoutes: HttpApp[UserEnv, HttpError] = HttpApp.collectM {
    case r @ Method.POST -> Root / "register" =>
      register(r)
    case r @ Method.POST -> Root / "login" =>
      login(r)
    case Method.GET -> Root =>
      fromAsset(Path("buckets.html"))
  }

  private def register(r: Request): ResponseM[UserEnv, HttpError] =
    for {
      loginRequest <- extractData[LoginRequest](r)
      _ <-
        UserRepoService
          .create(loginRequest.login, loginRequest.password)
          .mapError(err => HttpError.InternalServerError(cause = Some(err)))
    } yield Response.ok

  private def login(r: Request): ResponseM[UserEnv, HttpError] =
    for {
      loginRequest <- extractData[LoginRequest](r)
      _ <-
        UserRepoService
          .isValid(loginRequest.login, loginRequest.password)
          .foldM(
            _ => ZIO.fail(HttpError.BadRequest("login or password are wrong")),
            valid =>
              ZIO
                .fail(HttpError.BadRequest("login or password are wrong"))
                .unless(valid)
          )
      token <- JwtToken.encode(UserToken(loginRequest.login))
    } yield Response.HttpResponse(
      Status.OK,
      List(
        authorization(s"Bearer $token"),
        Header("Set-Cookie", s"token=$token;HttpOnly")
      ),
      HttpData.CompleteData(Chunk.fromArray(token.getBytes))
    )

}
