package ru.studyground.domain.buckets.task

import ru.studyground.BucketsTask
import ru.studyground.Repository
import ru.studyground.config.AppConfig
import zio.blocking.Blocking
import zio.cache.{Cache, Lookup}
import zio.console.Console
import zio.duration.durationInt
import zio.stream.ZStream
import zio.{Has, RIO, ZIO, ZLayer, ZManaged}
import ru.studyground.domain.buckets.task.RepoCodecs._

import java.util.UUID

final case class NotFound(uuid: UUID) extends Exception

trait BucketsTaskRepository {
  def get(uuid: UUID): RIO[Blocking, Option[BucketsTask]]
  def set(bucketsTask: BucketsTask): RIO[Blocking, Unit]
  def delete(uuid: UUID): RIO[Blocking, Unit]
  def getAfter(uuid: Option[UUID], take: Option[Int]): ZStream[Blocking, Throwable, BucketsTask]
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
  def getAfter(uuid: Option[UUID], take: Option[Int]
  ): ZStream[Has[BucketsTaskRepository] with Blocking, Throwable, BucketsTask] =
    ZStream.accessStream(_.get.getAfter(uuid, take))

  private def mkCache(rep: Repository[UUID, BucketsTask]) =
    Cache.make[UUID, Blocking, Throwable, BucketsTask](
      50,
      2.hours,
      Lookup(uuid =>
        for {
          mbBuck <- rep.get(uuid)
          res <- ZIO.fromOption(mbBuck).orElseFail(NotFound(uuid))
        } yield res
      )
    )

  val live: ZLayer[RepoEnv, Throwable, Has[BucketsTaskRepository]] =
    (for {
      config <- ZManaged.service[AppConfig]
      repository <-
        Repository.mkRepository[UUID, BucketsTask](config.bucketsTaskRepo)
      cache <- mkCache(repository).toManaged_
    } yield RocksBucketsTaskRepository(repository, cache)).toLayer
}

final case class RocksBucketsTaskRepository(
    rep: Repository[UUID, BucketsTask],
    cache: Cache[UUID, Throwable, BucketsTask]
) extends BucketsTaskRepository {

  override def get(uuid: UUID): RIO[Blocking, Option[BucketsTask]] =
    cache.get(uuid).map(Option(_)).catchSome {
      case NotFound(_) => ZIO.none
    }

  override def set(bucketsTask: BucketsTask): RIO[Blocking, Unit] =
    rep.put(bucketsTask.id.value, bucketsTask)

  override def getAfter(
      uuid: Option[UUID],
      take: Option[Int]
  ): ZStream[Blocking, Throwable, BucketsTask] = {
    val str = rep.asStream(uuid)
    take.fold(str)(str.take(_))
  }

  override def delete(uuid: UUID): RIO[Blocking, Unit] =
    cache.invalidate(uuid) *> rep.delete(uuid)
}