package ru.studyground

import com.raquo.laminar.api.L._
import com.raquo.waypoint._

object WelcomeApp {
  val MenuTitle = "Главная"

  val route: Route[_ <: Page, _] =
    Route.static(Page.Welcome, root / endOfSegments)

  def apply(): Div =
    div(
      "Добро пожаловать"
    )

}
