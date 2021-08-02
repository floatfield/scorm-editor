package ru
package studyground

import ru.studyground.domain.user.UserRepo
import ru.studyground.http._
import ru.studyground.jwt.JwtToken
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.blocking.Blocking
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.yaml.YamlConfig
import zio.console.{Console, putStrLn}
import zio.random.Random

import java.nio.file.Paths

case class AppConfig(
    secretKey: String,
    webDirs: List[String],
    userRepo: String
)

object BucketsServer extends App {

  val configDescr: ConfigDescriptor[AppConfig] = descriptor[AppConfig]

  val configLayer: Layer[ReadError[String], Has[AppConfig]] = YamlConfig.fromPath(
    Paths.get("/Users/feremeev/projects/temp/scalajs/buckets-data/app-config.yaml"),
    configDescr
  )

  val layer: ZLayer[Blocking with Random with Console with Has[AppConfig], Throwable, Has[UserRepo] with Has[JwtToken]] =
    UserRepo.live ++ JwtToken.live

  val text: HttpApp[Has[JwtToken], HttpError] = authorized {
    case Method.GET -> Root / "text" =>
      Response.text("some text")
  }

  val loggedText = authorizedM {
    case r @ (Method.GET -> Root / "logged") =>
      putStrLn(r.getBearerToken.getOrElse("no token")).ignore
        .as(Response.text("some text"))
  }

  val app = text +++ loggedText +++ http.static.static +++ http.user.userRoutes

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    Server.start(8080, app.silent).provideLayer((configLayer ++ ZEnv.live) >+> layer).exitCode
  }
}
