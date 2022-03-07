package ru.studyground

import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import org.scalajs.dom
import ru.studyground.Page.{Login, Welcome}
import ru.studyground.buckets.BucketsApp
import zio.json._

object MainApp {

  private val state = Var(MainAppState.empty)

  private val commandObserver: Observer[Command] = state.updater(Command.dispatch)

  private val routes = List(WelcomeApp.route, LoginApp.route) ++ BucketsApp.routes

  private val router = new Router[Page](
    routes = routes,
    getPageTitle = _ => "Желтые коробочки",
    serializePage = page => page.toJson,
    deserializePage = _.fromJson[Page].getOrElse(Welcome)
  )(
    $popStateEvent = windowEvents.onPopState,
    owner = unsafeWindowOwner
  )

  private val splitRender = SplitRender[Page, HtmlElement](router.$currentPage)
    .collectStatic(Welcome)(WelcomeApp())
    .collectStatic(Login)(
      LoginApp.renderLogin(state.signal.map(_.loginState), commandObserver, router)
    )
    .collectSignal[BucketsPage]($page => BucketsApp.render($page, router, state.signal, commandObserver))

  private val application = div(
    TopMenu(state.signal, commandObserver)(router),
    child <-- splitRender.$view
  )

  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app-container")
      render(appContainer, application)
    }(unsafeWindowOwner)
  }
}
