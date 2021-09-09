package ru.studyground

import org.flywaydb.core.Flyway
import ru.studyground.config.PostgresConfig
import zio.blocking.Blocking
import zio.{Has, RIO, Task, ZIO, ZLayer}

trait Migration {
  def migrate(): Task[Unit]
}

object Migration {
  def migrate(): RIO[Has[Migration], Unit] =
    ZIO.accessM(_.get.migrate())

  val live: ZLayer[Blocking with Has[PostgresConfig], Nothing, Has[Migration]] =
    ZLayer.fromServices[Blocking.Service, PostgresConfig, Migration] {
      case (blocking, config) =>
        new Migration {
          override def migrate(): Task[Unit] = {
              blocking.effectBlocking {
                val c = config.properties.withDefault(_ => "")
                Flyway
                  .configure()
                  .dataSource(
                    s"jdbc:postgresql://${c("dataSource.serverName")}:${c("dataSource.portNumber")}/${c("dataSource.databaseName")}",
                    c("dataSource.user"),
                    c("dataSource.password")
                  )
                  .load
                  .migrate
              }
          }
        }
    }
}
