package ru.studyground

import com.raquo.laminar.api.L._
import org.scalajs.dom

object MainApp {

  val application = div(
    div(
      cls("ui large top menu"),
      div(
        cls("ui container"),
        a(cls("active item"), "Категории"),
        a(cls("item"), "Тесты"),
        div(
          cls("right menu"),
          div(cls("item"), a(cls("ui button"), "Войти"))
        )
      )
    ),
    div(
      cls("ui middle aligned stackable grid container"),
      div(
        cls("row"),
        div(
          cls("eight wide column"),
          h3("Категории"),
          p("Создавайте категории желтые коробочки бесплатно")
        ),
        div(
          cls("four wide right floated column"),
          p("some what")
        )
      )
    )
  )

  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app-container")
      render(appContainer, application)
    }(unsafeWindowOwner)
  }
}
