package ru.studyground.buckets

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import zio.json._
import ru.studyground.{BucketsTask, BucketsTaskId}
import org.scalajs.dom.console

object Requests {

  val defaultHeaders = Map("Content-type" -> "application/json")

  def loadBucketTasks: EventStream[Either[String, List[BucketsTask]]] =
    AjaxEventStream
      .get(url = "/buckets", headers = defaultHeaders)
      .map(r => r.responseText.fromJson[List[BucketsTask]])
      .recover {
        case err: AjaxStreamError =>
          console.error(err.getMessage)
          Some(Left(err.xhr.responseText))
      }

  def removeBucketsTask(id: BucketsTaskId): EventStream[Option[String]] =
    AjaxEventStream
      .delete(url = s"/buckets/${id.value.toString}", headers = defaultHeaders)
      .mapTo(None)
      .recover {
        case err: AjaxStreamError =>
          console.error(err.getMessage)
          Some(Some(err.xhr.responseText))
      }
}