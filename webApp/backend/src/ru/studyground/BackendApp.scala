package ru.studyground

import ru.studyground.config.AppConfig
import ru.studyground.config.config.{live => configLayer}
import ru.studyground.domain.buckets.task.BucketsTaskRepository
import ru.studyground.domain.user.UserDBRepo
import ru.studyground.http.BucketsServer.startServer
import ru.studyground.jwt.JwtToken
import zio._
import zio.blocking.Blocking
import zio.config.syntax._
import zio.console.Console
import zio.random.Random

object BackendApp extends App {

  type AppEnv = Blocking with Random with Console with Has[AppConfig]

  private val postgresConfig = configLayer
    .narrow(_.postgresConfig.get)
    .mapError(_ => new Exception("no database config found"))

  private val migration = (Blocking.live ++ postgresConfig) >>> Migration.live

  private val userRepoLayer =
    (PasswordEncryption.live ++ (postgresConfig >>> datasource.hicari)) >>> UserDBRepo.live

  private val appConfigLayer = configLayer.narrow(_.appConfig)

  private val bucketsTaskRepoLayer =
    (Blocking.live ++ appConfigLayer ++ Console.live) >>> BucketsTaskRepository.live

  private val layer =
    userRepoLayer ++ JwtToken.live ++ bucketsTaskRepoLayer

  private val backendAppLayer =
    configLayer.narrow(_.appConfig) ++ Random.live >+> layer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    Migration.migrate().provideCustomLayer(migration).orDie *>
      startServer
        .provideCustomLayer(backendAppLayer ++ Blocking.live)
        .exitCode
  }
}
