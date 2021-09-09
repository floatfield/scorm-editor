package ru.studyground

import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import org.scalajs.dom
import ru.studyground.Page.{Login, Welcome}
import ru.studyground.buckets.BucketsApp
import zio.json._

object MainApp {

  val state = Var(MainAppState.empty)

  val logInObserver: Observer[String] =
    Observer[String](_ => router.pushState(Page.Welcome))

  val commandObserver: Observer[Command] = state.updater(Command.dispatch)

  val routes = List(WelcomeApp.route, LoginApp.route) ++ BucketsApp.routes

  val router = new Router[Page](
    routes = routes,
    getPageTitle = _ => "Желтые коробочки",
    serializePage = page => page.toJson,
    deserializePage = s => s.fromJson[Page].getOrElse(Welcome)
  )(
    $popStateEvent = windowEvents.onPopState,
    owner = unsafeWindowOwner
  )

  val splitRender = SplitRender[Page, HtmlElement](router.$currentPage)
    .collectStatic(Welcome)(WelcomeApp())
    .collectStatic(Login)(
      LoginApp.renderLogin(state.signal.map(_.loginState), commandObserver)
    )
    .collectSignal[BucketsPage]($page => BucketsApp.render($page, router))

  val application = div(
    TopMenu(state.signal, commandObserver)(router),
    child <-- splitRender.$view,
    state.signal.map(_.token) --> logInObserver.contracollect[Option[String]] {
      case Some(token) => token
    }
  )

  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app-container")
      render(appContainer, application)
    }(unsafeWindowOwner)
  }
}
