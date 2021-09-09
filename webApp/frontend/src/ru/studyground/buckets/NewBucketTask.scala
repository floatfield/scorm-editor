package ru.studyground.buckets

import com.raquo.laminar.api.L._
import org.scalajs.dom.FileReader
import ru.studyground.common.TextFileInput

object NewBucketTask {
  def render(): Div = {
    val newBucketTaskId = "new-bucket-task"
    div(
      cls("ui three column centered grid"),
      cls("new-buckets"),
      div(
        cls("column"),
        form(
          cls("ui form"),
          div(
            cls("field"),
            label("Название"),
            input(
              typ("text"),
              name("name")
            )
          ),
          div(
            cls("field"),
            label("Описание"),
            textArea()
          ),
          div(
            cls("field"),
            TextFileInput(
              newBucketTaskId,
              Observer[String] { contents =>
                println(contents)
              }
            ),
            label(
              forId(newBucketTaskId),
              cls("ui basic blue button"),
              "Выберите файл с заданием"
            )
          )
        )
      )
    )
  }
}
