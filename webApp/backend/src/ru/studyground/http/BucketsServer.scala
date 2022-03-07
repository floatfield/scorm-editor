package ru.studyground.http

import zhttp.service.Server
import ContentRoutes.{routes => contentRoutes}
import ru.studyground.http.BucketTasks.{routes => bucketsTasks}
import ru.studyground.http.Assignments.{routes => assignments}

object BucketsServer {
  private val app = static.static ++ bucketsTasks ++ user.userRoutes ++ contentRoutes ++ assignments

  val startServer = Server.start(8080, app.silent)
}
