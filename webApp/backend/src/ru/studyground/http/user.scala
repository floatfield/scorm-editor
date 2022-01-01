package ru.studyground.http

import ru.studyground.domain.user.UserRepoService
import ru.studyground.jwt.{JwtToken, UserToken}
import zhttp.http.Header.authorization
import zhttp.http._
import zhttp._
import zio.blocking.Blocking
import zio.json._
import zio.{Chunk, Has, ZIO}
import java.nio.charset.Charset

case class LoginRequest(login: String, password: String)

object LoginRequest {
  implicit val encoder: JsonEncoder[LoginRequest] =
    DeriveJsonEncoder.gen[LoginRequest]
  implicit val decoder: JsonDecoder[LoginRequest] =
    DeriveJsonDecoder.gen[LoginRequest]
}

object user {

  type UserEnv =
    Has[UserRepoService]
      with Has[JwtToken]
      with Blocking

  val userRoutes: HttpApp[UserEnv, HttpError] =
    jsonContent[UserEnv] >>> Http.collectM[Request] {
      case r @ Method.POST -> !! / "register" =>
        register(r)
      case r @ Method.POST -> !! / "login" =>
        login(r)
      case Method.POST -> !! / "logout" =>
        logout
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
    } yield Response(
      Status.OK,
      List(
        authorization(s"Bearer $token"),
        Header("Set-Cookie", s"token=$token;HttpOnly")
      ),
      HttpData.fromText(token)
    )

  private val logout: ResponseM[UserEnv, HttpError] =
    ZIO.succeed(Response.ok.addCookie(Cookie("token", "").clear))

}
