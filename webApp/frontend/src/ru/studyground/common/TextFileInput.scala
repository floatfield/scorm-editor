package ru.studyground.common

import com.raquo.laminar.api.L._
import com.raquo.airstream.core.Observer
import org.scalajs.dom.FileReader

object TextFileInput {
  def apply(id: String, observer: Observer[String]): Input =
    input(
      idAttr(id),
      cls("fileinput"),
      typ("file"),
      inContext { thisNode =>
        val (fileContents, fileContentsObserver) =
          EventStream.withObserver[String]
        val $contents = thisNode.events(onChange).flatMap { _ =>
          val files = thisNode.ref.files
          val reader = new FileReader
          reader.onload = _ =>
            fileContentsObserver
              .onNext(reader.result.asInstanceOf[String])
          reader.onerror = _ =>
            fileContentsObserver.onError(new Exception(reader.error.message))
          reader.readAsText(files(0))
          fileContents
        }
        $contents --> observer
      }
    )
}