package ru.studyground.http

import ru.studyground.{BucketsTask, BucketsTaskDTO, BucketsTaskId}
import ru.studyground.domain.buckets.task.BucketsTaskRepository
import ru.studyground.jwt.JwtToken
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.blocking.Blocking
import zio.console.putStrLn
import zio.random._

object BucketsServer {

  private val text: HttpApp[Has[JwtToken], HttpError] = authorized {
    case Method.GET -> Root / "text" =>
      Response.text("some text")
  }

  private val loggedText = authorizedM {
    case r @ Method.GET -> Root / "logged" =>
      putStrLn(r.getBearerToken.getOrElse("no token")).ignore
        .as(Response.text("some text"))
  }

  private val newBucketsTask =
    authorizedM[Random with Has[BucketsTaskRepository] with Blocking] {
      case r @ Method.POST -> Root / "buckets" =>
        for {
          bucketsTaskDTO <- extractData[BucketsTaskDTO](r)
          uuid <- nextUUID
          bucketsTask <-
            ZIO
              .fromEither(
                BucketsTask
                  .fromBucketsTaskDTO(bucketsTaskDTO, BucketsTaskId(uuid))
              )
              .mapError(pe => HttpError.BadRequest(pe.msg))
          _ <-
            BucketsTaskRepository
              .set(bucketsTask)
              .mapError(err => HttpError.InternalServerError(cause = Some(err)))
        } yield Response.ok
    }

  private val app =
    text +++ loggedText +++ static.static +++ user.userRoutes +++ newBucketsTask

  val startServer = Server.start(8080, app.silent)
}
