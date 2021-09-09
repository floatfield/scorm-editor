package ru.studyground.config

import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.yaml.YamlConfig
import zio.{Has, Layer}

import java.nio.file.Paths

case class Application(
    appConfig: AppConfig,
    dbConfig: DbConfig,
    h2Config: Option[H2Config],
    postgresConfig: Option[PostgresConfig]
)

case class AppConfig(
    secretKey: String,
    webDirs: List[String],
    userRepo: String,
    bucketsTaskRepo: String
)

case class DbConfig(
    url: String,
    user: String,
    password: String
)

case class H2Config(
    properties: Map[String, String]
)

case class PostgresConfig(
    properties: Map[String, String]
)

object config {

  val configDescr: ConfigDescriptor[Application] = descriptor[Application]

  val live: Layer[ReadError[String], Has[Application]] =
    YamlConfig.fromPath(
      Paths.get(
        "/Users/feremeev/projects/temp/scalajs/buckets-data/app-config.yaml"
      ),
      configDescr
    )
}
