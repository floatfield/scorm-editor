package ru.studyground.http

import ru.studyground._
import ru.studyground.domain.buckets.task.BucketsTaskRepository
import zhttp.http.HttpError.BadRequest
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.blocking.Blocking
import zio.random._
import zio.json._
import ContentRoutes.{routes => contentRoutes}
import ru.studyground.domain.buckets.task.BucketsTaskRepository.getAll
import ru.studyground.jwt.JwtToken

import java.util.UUID
import scala.util.Try

sealed trait BucketsTaskEvent

final case class AddTasks(tasks: List[BucketsTask]) extends BucketsTaskEvent
final case class UpdateTask(task: BucketsTask) extends BucketsTaskEvent
final case class RemoveTasks(ids: List[BucketsTaskId]) extends BucketsTaskEvent

object BucketsServer {

  type Env = Random with Has[BucketsTaskRepository] with Blocking with Has[
    Hub[BucketsTaskEvent]
  ] with Has[JwtToken]

  private val bucketsTasks =
    jsonContent[Env] >>> authorizedM[Env] {
      case Method.GET -> !! / "buckets" =>
        getAllBucketTasks
      case r @ Method.POST -> !! / "buckets" =>
        createBucketsTask(r)
      case r @ Method.PUT -> !! / "buckets" / uuid =>
        ZIO
          .fromTry(Try(UUID.fromString(uuid)))
          .orElseFail(
            BadRequest(s"uuid $uuid has wrong format")
          )
          .flatMap(uuid => setBucketsTask(uuid, r))
      case Method.DELETE -> !! / "buckets" / uuid =>
        ZIO
          .fromTry(Try(UUID.fromString(uuid)))
          .orElseFail(
            BadRequest(s"uuid $uuid has wrong format")
          ) >>= removeBucketsTask
    }

  private def getAllBucketTasks: ZIO[Has[BucketsTaskRepository] with Blocking, HttpError, UResponse] = {
    getAll.mapBoth(
      _ => HttpError.InternalServerError(),
      tasks => Response.text(tasks.toJson)
    )
  }

  private def createBucketsTask(r: Request): ZIO[Random with Has[
    BucketsTaskRepository
  ] with Blocking with Has[Hub[BucketsTaskEvent]], HttpError, UResponse] = {
    for {
      uuid <- nextUUID
      bucketsTask <- setTask(uuid, r)
      _ <- ZIO.accessM[Has[Hub[BucketsTaskEvent]]](
        _.get.publish(AddTasks(List(bucketsTask)))
      )
    } yield Response.ok
  }

  private def setTask(
      uuid: UUID,
      r: Request
  ): ZIO[Has[BucketsTaskRepository] with Blocking, HttpError, BucketsTask] =
    for {
      bucketsTaskDTO <- extractData[BucketsTaskDTO](r)
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
    } yield bucketsTask

  private def setBucketsTask(
      uuid: UUID,
      r: Request
  ): ZIO[Has[BucketsTaskRepository] with Blocking with Has[
    Hub[BucketsTaskEvent]
  ], HttpError, UResponse] =
    for {
      bucketsTask <- setTask(uuid, r)
      _ <- ZIO.accessM[Has[Hub[BucketsTaskEvent]]](
        _.get.publish(UpdateTask(bucketsTask))
      )
    } yield Response.ok

  private def removeBucketsTask(
      uuid: UUID
  ): ZIO[Has[BucketsTaskRepository] with Blocking, HttpError, UResponse] =
    BucketsTaskRepository
      .delete(uuid)
      .bimap(_ => HttpError.InternalServerError(), _ => Response.ok)

  private val app = static.static ++ bucketsTasks ++ user.userRoutes ++ contentRoutes

  val startServer = Server.start(8080, app.silent)
}
