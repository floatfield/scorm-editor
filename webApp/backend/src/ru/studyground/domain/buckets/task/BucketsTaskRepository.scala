package ru.studyground.domain.buckets.task

import io.getquill.context.qzio.ImplicitSyntax.Implicit
import io.getquill.context.ZioJdbc._
import io.getquill.{CamelCase, PostgresZioJdbcContext}
import ru.studyground.{
  BucketName,
  BucketNames,
  BucketsTask,
  BucketsTaskId,
  FullBucket
}
import ru.studyground.config.AppConfig
import zio.blocking.Blocking
import zio.console.Console
import zio.{Has, RIO, Task, ZIO, ZLayer}
import zio.json._

import java.io.Closeable
import java.util.UUID
import javax.sql.DataSource

final case class NotFound(uuid: UUID) extends Exception

trait BucketsTaskRepository {
  def get(uuid: UUID): RIO[Blocking, Option[BucketsTask]]
  def set(bucketsTask: BucketsTask): RIO[Blocking, Unit]
  def delete(uuid: UUID): RIO[Blocking, Unit]
  def getAll: ZIO[Blocking, Throwable, List[BucketsTask]]
}

object BucketsTaskRepository {
  type RepoEnv = Has[AppConfig] with Blocking with Console

  def get(
      uuid: UUID
  ): RIO[Has[BucketsTaskRepository] with Blocking, Option[BucketsTask]] =
    ZIO.accessM(_.get.get(uuid))
  def set(
      bucketsTask: BucketsTask
  ): RIO[Has[BucketsTaskRepository] with Blocking, Unit] =
    ZIO.accessM(_.get.set(bucketsTask))
  def delete(uuid: UUID): RIO[Has[BucketsTaskRepository] with Blocking, Unit] =
    ZIO.accessM(_.get.delete(uuid))
  def getAll: ZIO[Has[BucketsTaskRepository] with Blocking, Throwable, List[
    BucketsTask
  ]] =
    ZIO.accessM(_.get.getAll)

  val dbLayer: ZLayer[Has[DataSource with Closeable], Nothing, Has[
    BucketsTaskRepository
  ]] =
    ZLayer.fromService(BucketsDBRepository)
}

final case class FullBucketParsingException(err: String) extends Exception

final case class BucketsTaskDTO(
    id: UUID,
    name: String,
    maxItemsNumber: Int,
    description: String,
    bucketNames: Seq[String],
    fullBuckets: String
) { self =>
  val toBucketsTask: Task[BucketsTask] =
    ZIO
      .fromEither(self.fullBuckets.fromJson[Seq[FullBucket]])
      .map(buckets =>
        BucketsTask(
          id = BucketsTaskId(id),
          name = self.name,
          maxItemsNumber = maxItemsNumber,
          description = self.description,
          bucketNames = BucketNames(self.bucketNames.map(BucketName(_))),
          fullBuckets = buckets
        )
      )
      .mapError(FullBucketParsingException)
}

object BucketsTaskDTO {
  def fromBucketsTask(bucketsTask: BucketsTask): BucketsTaskDTO =
    BucketsTaskDTO(
      id = bucketsTask.id.value,
      name = bucketsTask.name,
      maxItemsNumber = bucketsTask.maxItemsNumber,
      description = bucketsTask.description,
      bucketNames = bucketsTask.bucketNames.names.map(_.name),
      fullBuckets = bucketsTask.fullBuckets.toJson
    )
}

final case class BucketsDBRepository(
    ds: DataSource with Closeable
) extends BucketsTaskRepository {

  object Ctx extends PostgresZioJdbcContext(CamelCase)
  import Ctx._

  val bucketsTasks = quote {
    querySchema[BucketsTaskDTO]("bucketsTasks")
  }

  implicit val env: Implicit[Has[DataSource with Closeable]] = Implicit(Has(ds))

  override def get(uuid: UUID): RIO[Blocking, Option[BucketsTask]] =
    Ctx
      .run(quote(bucketsTasks).filter(_.id == lift(uuid)))
      .map(_.headOption)
      .flatMap {
        case None      => ZIO.none
        case Some(dto) => dto.toBucketsTask.map(Some(_))
      }
      .implicitDS

  override def set(bucketsTask: BucketsTask): RIO[Blocking, Unit] =
    (Ctx.run(
      quote(
        bucketsTasks.insert(lift(BucketsTaskDTO.fromBucketsTask(bucketsTask)))
      )
    ) <> Ctx.run(
      bucketsTasks.update(lift(BucketsTaskDTO.fromBucketsTask(bucketsTask)))
    )).unit.implicitDS

  override def delete(uuid: UUID): RIO[Blocking, Unit] =
    Ctx
      .run(
        quote(bucketsTasks.filter(_.id == lift(uuid)).delete)
      )
      .unit
      .implicitDS

  override def getAll: ZIO[Blocking, Throwable, List[BucketsTask]] =
    Ctx
      .run(quote(bucketsTasks))
      .flatMap(xs => ZIO.foreach(xs)(_.toBucketsTask))
      .implicitDS

}
