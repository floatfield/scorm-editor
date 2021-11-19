package ru.studyground

import com.raquo.airstream.core.EventStream.toCombinableStream
import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import io.laminext.websocket._
import org.scalajs.dom
import org.scalajs.dom.console
import ru.studyground.Command.{Logout, SetToken}
import ru.studyground.Page.{Login, Welcome}
import ru.studyground.buckets.BucketsApp
import zio.json._

object MainApp {

  private val ws = WebSocket.url(s"ws://${dom.document.location.host}/buckets-tasks").text[ServerMessage, ClientMessage](
    _.toJson,
    s => s.fromJson[ServerMessage].fold(s => Left(new Exception(s)), Right(_))
  ).build(managed = false)

  private val state = Var(MainAppState.empty)

  private val sideEffectObserver: Observer[Command] = Observer[Command]{
    case Logout =>
      ws.disconnectNow()
    case SetToken(_) =>
      router.pushState(Page.Welcome)
    case _ =>
  }

  private val commandObserver: Observer[Command] = Observer.combine(
    sideEffectObserver,
    state.updater(Command.dispatch)
  )

  private val routes = List(WelcomeApp.route, LoginApp.route) ++ BucketsApp.routes

  private val router = new Router[Page](
    routes = routes,
    getPageTitle = _ => "Желтые коробочки",
    serializePage = page => page.toJson,
    deserializePage = s => s.fromJson[Page].getOrElse(Welcome)
  )(
    $popStateEvent = windowEvents.onPopState,
    owner = unsafeWindowOwner
  )

  private val splitRender = SplitRender[Page, HtmlElement](router.$currentPage)
    .collectStatic(Welcome)(WelcomeApp())
    .collectStatic(Login)(
      LoginApp.renderLogin(state.signal.map(_.loginState), commandObserver)
    )
    .collectSignal[BucketsPage]($page => BucketsApp.render($page, router, state.signal, commandObserver, ws))

  private val application = div(
    state.signal.map(_.token) --> ws.reconnect.contracollect[Option[String]] {
      case Some(_) =>
        println("connecting to socket server")
    },
    ws.connected.sample(state.signal.map(_.token)) --> ws.send.contracollect[Option[String]] {
      case Some(t) => Token(t)
    },
    ws.received.withCurrentValueOf(state.signal) --> state.writer.contracollect[(ServerMessage, MainAppState)] {
      case (AddBucketsTasks(tasks), s) =>
        s.prependBucketsTasks(tasks)
      case (UpdateBucketsTask(task), s) =>
        s.updateBucketsTask(task)
      case (RemoveBucketsTasks(ids), s) =>
        s.removeBucketsTasks(ids)
    },
    ws.errors --> Observer[Throwable](err => console.error(err)),
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
