package ru.studyground.common

import com.raquo.laminar.api.L._
import ru.studyground.FormState

object Form {
  def apply(
      content: Div,
      $formState: Signal[FormState],
      closeErrorObserver: Observer[Unit]
  ): Div =
    div(
      cls("ui three column centered grid"),
      div(
        cls("column"),
        form(
          cls("ui form"),
          cls <-- $formState.map(s =>
            List(
              "loading" -> (s == FormState.Loading),
              "error" -> s.isInstanceOf[FormState.Error]
            )
          ),
          child <-- $formState.map {
            case FormState.Error(msg) => errorMessage(msg, closeErrorObserver)
            case _                    => emptyNode
          },
          content
        )
      )
    )

  private def errorMessage(
      msg: String,
      closeErrorObserver: Observer[Unit]
  ): Div = {
    div(
      cls("ui error message fadeout"),
      i(
        cls("close icon"),
        onClick.mapTo(()) --> closeErrorObserver
      ),
      div(cls("header"), "Ошибка"),
      p(msg),
      onAnimationEnd.mapTo(()) --> closeErrorObserver
    )
  }
}
