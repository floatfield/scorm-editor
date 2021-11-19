package ru.studyground.config

import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.yaml.YamlConfig
import zio.{Has, Layer}

import java.nio.file.Paths

case class Application(
    appConfig: AppConfig,
    postgresConfig: Option[PostgresConfig]
)

case class AppConfig(
    secretKey: String,
    webDirs: Map[String, String],
    userRepo: String,
    bucketsTaskRepo: String
)

case class PostgresConfig(
    properties: Map[String, String]
)

object config {

  val configDescr: ConfigDescriptor[Application] = descriptor[Application]

  val live: Layer[ReadError[String], Has[Application]] =
    YamlConfig.fromPath(
      Paths.get(
        "/home/bitterlife/rubbish_heap/rubbish/botva/scala/scorm-editor/webApp/backend/resources/buckets-data/app-config.yaml"
      ),
      configDescr
    )
}
