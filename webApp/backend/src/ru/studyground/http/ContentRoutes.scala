package ru.studyground.http

import zhttp.http._
import zio.blocking.Blocking
import zio.{Has, ZIO}

object ContentRoutes {
  type Env = Has[StaticDirectorySet] with Blocking

  val routes: HttpApp[Env, HttpError] = HttpApp.collectM {
    case Method.GET -> Root =>
      for {
        d <-
          ZIO
            .access[Has[StaticDirectorySet]](_.get.getDirValue("static"))
            .flatMap(ZIO.fromOption(_))
            .orElseFail(HttpError.InternalServerError())
        res <- fromAsset(s"$d/buckets.html")
      } yield res
  }
}