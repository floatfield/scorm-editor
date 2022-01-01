package ru.studyground.config

import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe.TypesafeConfig
import zio.{Has, Layer}

case class Application(
    appConfig: AppConfig,
    postgresConfig: PostgresConfig
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
    TypesafeConfig.fromDefaultLoader(configDescr)
}
