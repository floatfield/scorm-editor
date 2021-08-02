package ru
package studyground
package http

import zhttp.http._
import zio.Has
import zio.blocking.Blocking

object static {

  val static: HttpApp[Blocking with Has[AppConfig], HttpError] =
    HttpApp.collectM {
      case r if r.method == Method.GET && r.url.path.toList.head == "static" =>
        fromAsset(Path(r.url.path.toList.tail))
    }
}
