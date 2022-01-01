package ru.studyground.http

import ru.studyground.jwt.JwtToken
import zhttp.http._
import zhttp._
import zio.blocking.Blocking
import zio.cache.{Cache, Lookup}
import zio.duration.durationInt
import zio.stream.ZSink
import zio.{Has, URIO, ZIO}

object ContentRoutes {
  type Env = Has[StaticDirectorySet] with Blocking with Has[JwtToken]

  private val templateCache =
    Cache.make[String, Blocking, HttpError, String](
      capacity = 50,
      10.hours,
      Lookup(templateName =>
        readResource(templateName).flatMap { bs =>
          bs.runCollect.map(bytes => new String(bytes.toArray))
        }
      )
    )

  private val staticDir = ZIO
    .access[Has[StaticDirectorySet]](_.get.getDirValue("static"))
    .flatMap(ZIO.fromOption(_))
    .orElseFail(HttpError.InternalServerError())

  private val authed = authorizedM[Env] {
    case Method.GET -> !! =>
      renderApp
    case Method.GET -> !! / "buckets" =>
      renderApp
    case Method.GET -> !! / "buckets" / uuid =>
      for {
        d <- staticDir
        cache <- templateCache
        template <- cache.get(s"$d/buckets-task.mustache")
        res = template.replace("{{bucketsTaskId}}", uuid)
      } yield Response.text(res)
  }

  private val unauthed = Http.collectM[Request] {
    case Method.GET -> !! / "login" =>
      renderApp
  }

  val routes: HttpApp[Env, HttpError] = authed ++ unauthed

  private val renderApp =
    for {
      d <- staticDir
      res <- fromAsset(s"$d/buckets.html")
    } yield res
}