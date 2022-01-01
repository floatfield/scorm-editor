package ru.studyground

import ru.studyground.config.AppConfig
import ru.studyground.config.config.{live => configLayer}
import ru.studyground.domain.buckets.task.BucketsTaskRepository
import ru.studyground.domain.user.UserDBRepo
import ru.studyground.http.BucketsServer.startServer
import ru.studyground.http.BucketsTaskEvent
import ru.studyground.jwt.JwtToken
import zio._
import zio.blocking.Blocking
import zio.config.syntax._
import zio.console.Console
import zio.random.Random

object BackendApp extends App {

  type AppEnv = Blocking with Random with Console with Has[AppConfig]

  private val postgresConfig = configLayer
    .narrow(_.postgresConfig)

  private val migration = (Blocking.live ++ postgresConfig) >>> Migration.live

  private val encryptorLayer = Random.live >>> PasswordEncryption.live

  private val userRepoLayer =
    (encryptorLayer ++ (postgresConfig >>> datasource.hicari)) >>> UserDBRepo.live

  private val appConfigLayer = configLayer.narrow(_.appConfig)

  private val staticDirectoryLayer = appConfigLayer >>> http.static.staticDirectorySetLayer

  private val bucketsTaskRepoLayer =
    (postgresConfig >>> datasource.hicari) >>> BucketsTaskRepository.dbLayer

  private val jwtTokenLayer = appConfigLayer >>> JwtToken.live

  private val bucketsTaskEventLayer = Hub.bounded[BucketsTaskEvent](30).toLayer

  private val backendAppLayer =
    Blocking.live ++
      jwtTokenLayer ++
      Random.live ++
      userRepoLayer ++
      jwtTokenLayer ++
      bucketsTaskRepoLayer ++
      bucketsTaskEventLayer ++
      appConfigLayer ++
      staticDirectoryLayer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    Migration.migrate().provideCustomLayer(migration).orDie *>
      startServer
        .provideCustomLayer(backendAppLayer)
        .exitCode
  }
}
