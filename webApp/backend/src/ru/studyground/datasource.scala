package ru.studyground

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import ru.studyground.config.PostgresConfig
import zio.{Has, ZIO, ZLayer, ZManaged}

import java.io.Closeable
import java.util.Properties
import javax.sql.DataSource

object datasource {
  val hicari
      : ZLayer[Has[PostgresConfig], Throwable, Has[DataSource with Closeable]] =
    ZLayer.fromServiceManaged(config =>
      ZManaged.make(
        ZIO.effect {
          val props = new Properties()
          config.properties.foreach { case (prop, value) => props.setProperty(prop, value) }
          new HikariDataSource(new HikariConfig(props))
        }
      )(ds => ZIO.effect(ds.close()).orDie)
    )
}
