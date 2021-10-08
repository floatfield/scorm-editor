package ru.studyground.http

import ru.studyground.config.AppConfig
import zhttp.http._
import zio.{Has, ZIO, ZLayer}
import zio.blocking.Blocking

trait StaticDirectorySet {
  val dirNames: Set[String]
  def getDirValue(name: String): Option[String]
}

object static {

  val staticDirectorySetLayer: ZLayer[Has[AppConfig], Nothing, Has[StaticDirectorySet]] =
    ZLayer.fromService[AppConfig, StaticDirectorySet](appConfig =>
      new StaticDirectorySet {
        override val dirNames: Set[String] = appConfig.webDirs.keySet
        override def getDirValue(name: String): Option[String] = appConfig.webDirs.get(name)
      }
    )

  val static: HttpApp[Blocking with Has[StaticDirectorySet], HttpError] =
    Http.fromEffectFunction[Request](staticPrefix) >>> Http.collectM[(Option[String], Request)] {
      case (Some(path), r) if r.method == Method.GET =>
        fromAsset(s"$path/${r.url.path.toList.tail.mkString("/")}")
    }

  private def staticPrefix(r: Request): ZIO[Has[StaticDirectorySet], Nothing, (Option[String], Request)] =
    ZIO.access[Has[StaticDirectorySet]]{ s =>
      val pref = r.url.path.toList.headOption
      (pref.flatMap(p => s.get.getDirValue(p)), r)
    }
}
