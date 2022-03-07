package ru.studyground.http

import ru.studyground._
import ru.studyground.domain.buckets.task.BucketsTaskParser.parseBucketsTask
import ru.studyground.domain.buckets.task.BucketsTaskRepository
import zhttp.http.HttpError.BadRequest
import zhttp.http._
import zio._
import zio.blocking.Blocking
import zio.random._
import zio.json._
import ru.studyground.domain.buckets.task.BucketsTaskRepository.getAll
import ru.studyground.jwt.JwtToken

import java.util.UUID
import scala.util.Try

object BucketTasks {
  type Env = Random
    with Has[BucketsTaskRepository]
    with Blocking
    with Has[JwtToken]

  val routes
      : Http[Env with Has[JwtToken], HttpError, Request, Response[Env with Has[
        JwtToken
      ], HttpError]] =
    jsonContent[Env] >>> authorizedM[Env] {
      case Method.GET -> !! / "buckets" =>
        getAllBucketTasks
      case r @ Method.POST -> !! / "buckets" =>
        createBucketsTask(r)
      case Method.GET -> !! / "buckets" / uuid =>
        ZIO
          .fromTry(Try(UUID.fromString(uuid)))
          .orElseFail(
            BadRequest(s"uuid $uuid has wrong format")
          )
          .flatMap(getBucketTask)
      case r @ Method.PUT -> !! / "buckets" / uuid =>
        ZIO
          .fromTry(Try(UUID.fromString(uuid)))
          .orElseFail(
            BadRequest(s"uuid $uuid has wrong format")
          )
          .flatMap(setBucketsTask(_, r))
      case Method.DELETE -> !! / "buckets" / uuid =>
        ZIO
          .fromTry(Try(UUID.fromString(uuid)))
          .orElseFail(
            BadRequest(s"uuid $uuid has wrong format")
          ) >>= removeBucketsTask
    }

  private def getAllBucketTasks
      : ZIO[Has[BucketsTaskRepository] with Blocking, HttpError, UResponse] = {
    getAll.mapBoth(
      _ => HttpError.InternalServerError(),
      tasks => Response.jsonString(tasks.toJson)
    )
  }

  private def createBucketsTask(r: Request): ZIO[Random with Has[
    BucketsTaskRepository
  ] with Blocking, HttpError, UResponse] =
    nextUUID.flatMap(setTask(_, r)).as(Response.ok)

  private def setTask(
      uuid: UUID,
      r: Request
  ): ZIO[Has[BucketsTaskRepository] with Blocking, HttpError, BucketsTask] =
    for {
      bucketsTaskDTO <- extractData[BucketsTaskDTO](r)
      bucketsTask <-
        ZIO
          .fromEither(
            parseBucketsTask(bucketsTaskDTO, BucketsTaskId(uuid))
          )
          .mapError(pe => HttpError.BadRequest(pe.msg))
      _ <-
        BucketsTaskRepository
          .set(bucketsTask)
          .mapError(err => HttpError.InternalServerError(cause = Some(err)))
    } yield bucketsTask

  private def setBucketsTask(
      uuid: UUID,
      r: Request
  ): ZIO[Has[BucketsTaskRepository] with Blocking, HttpError, UResponse] =
    setTask(uuid, r).as(Response.ok)

  private def removeBucketsTask(
      uuid: UUID
  ): ZIO[Has[BucketsTaskRepository] with Blocking, HttpError, UResponse] =
    BucketsTaskRepository
      .delete(uuid)
      .bimap(_ => HttpError.InternalServerError(), _ => Response.ok)

  private def getBucketTask(uuid: UUID): ZIO[Has[
    BucketsTaskRepository
  ] with Blocking with Random, HttpError, UResponse] =
    for {
      mbTask <-
        BucketsTaskRepository
          .get(uuid)
          .orElseFail(HttpError.InternalServerError())
      task <-
        ZIO
          .fromOption(mbTask)
          .orElseFail(HttpError.NotFound(Path.End / "buckets" / uuid.toString))
    } yield Response.jsonString(task.toJson)
}
