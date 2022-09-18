package ru.studyground

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import org.scalajs.dom.console
import ru.studyground.MainApp.bucketsServer
import zio.json._

package object solver {
  def assesReq(
      id: BucketsTaskId,
      answers: List[BucketDTO],
      values: List[String]
  ): EventStream[Either[String, AssessmentResult]] =
    AjaxEventStream
      .post(
        url = s"$bucketsServer/assess",
        headers = Map("Content-Type" -> "application/json"),
        data = AnswerDTO(id, answers, values).toJson
      )
      .map(r => r.responseText.fromJson[AssessmentResult])
      .recover {
        case err: AjaxStreamError =>
          console.error(err.getMessage)
          Some(Left(err.xhr.responseText))
      }
}
