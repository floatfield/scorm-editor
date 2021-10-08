package ru.studyground.http

import ru.studyground._
import ru.studyground.domain.buckets.task.BucketsTaskRepository
import ru.studyground.jwt.JwtToken
import zhttp.http._
import zhttp.socket._
import zio.blocking.Blocking
import zio.json._
import zio.stream.ZStream
import zio.{Has, Hub, ZIO}

object WsRoutes {

  type Env = Has[JwtToken]
    with Has[Hub[BucketsTaskEvent]]
    with Has[BucketsTaskRepository]
    with Blocking

  private val bucketTasks: ZStream[Has[Hub[BucketsTaskEvent]] with Has[
    BucketsTaskRepository
  ] with Blocking, Throwable, BucketsTaskEvent] =
    ZStream.fromEffect(BucketsTaskRepository.getAll).map(AddTasks) ++
      ZStream.accessStream[Has[Hub[BucketsTaskEvent]]](h =>
        ZStream.fromHub(h.get)
      )

  private val extractClientMessage
      : PartialFunction[WebSocketFrame, Either[String, ClientMessage]] = {
    case WebSocketFrame.Text(s) => s.fromJson[ClientMessage]
  }

  private val handleClientMessage: PartialFunction[
    Either[String, ClientMessage],
    ZStream[Env, Throwable, WebSocketFrame]
  ] = {
    case Right(Token(t)) =>
      ZStream
        .fromEffect(
          JwtToken.validate(t)
        )
        .drain ++
        bucketTasks.map {
          case AddTasks(ts)     => toWebSocketFrame(AddBucketsTasks(ts))
          case RemoveTasks(ids) => toWebSocketFrame(RemoveBucketsTasks(ids))
          case UpdateTask(task) => toWebSocketFrame(UpdateBucketsTask(task))
        }
    case Right(RemoveTask(id)) =>
      ZStream
        .fromEffect(
          BucketsTaskRepository.delete(id.value) *>
            ZIO.accessM[Has[Hub[BucketsTaskEvent]]](
              _.get.publish(RemoveTasks(List(id)))
            )
        )
        .drain
  }

  private def toWebSocketFrame(m: ServerMessage): WebSocketFrame =
    WebSocketFrame.text(m.toJson)

  private val token = Socket.collect[WebSocketFrame](
    extractClientMessage andThen handleClientMessage
  )

  private val socketApp = SocketApp.message(token)

  val routes = HttpApp.collect {
    case Method.GET -> Root / "buckets-tasks" => socketApp
  }
}
