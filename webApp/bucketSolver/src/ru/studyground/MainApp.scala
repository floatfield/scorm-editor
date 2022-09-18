package ru.studyground

import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalajs.dom.console
import ru.studyground.Command.Start
import ru.studyground.State._
import ru.studyground.solver.BucketsSolve.{application => inProgress}
import zio.json._

import java.util.UUID

object MainApp {

  val bucketsTaskId: BucketsTaskId = BucketsTaskId(
    UUID.fromString(Globals.bucketsTaskId)
  )

  val bucketsServer: String = Globals.bucketsServer

  val loadAssignmentReq: EventStream[Either[String, BucketsAssignment]] =
    AjaxEventStream
      .get(
        url = s"$bucketsServer/${bucketsTaskId.value}",
        headers = Map("Content-Type" -> "application/json")
      )
      .map(r => r.responseText.fromJson[BucketsAssignment])
      .recover {
        case err: AjaxStreamError =>
          console.error(err.getMessage)
          Some(Left(err.xhr.responseText))
      }

  val state: Var[State] = Var(Loading(bucketsTaskId))

  val observer: Observer[Command] = state.updater(Command.dispatch)

  val loading: Div = div(
    cls("ui loading segment bucket-loader")
  )

  val application: Div = div(
    loadAssignmentReq --> observer
      .contracollect[Either[String, BucketsAssignment]] {
        case Right(assignment) => Start(assignment)
      },
    child <-- state.signal.map {
      case Loading(_)             => loading
      case Solving(assignment)    => inProgress(assignment)
    }
  )

  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app-container")
      render(appContainer, application)
    }(unsafeWindowOwner)
  }
}

sealed trait State

object State {
  final case class Loading(id: BucketsTaskId) extends State
  final case class Solving(assignment: BucketsAssignment) extends State
}

sealed trait Command

object Command {
  final case class Start(assignment: BucketsAssignment) extends Command
  def dispatch(state: State, c: Command): State =
    (state, c) match {
      case (Loading(_), Start(assignment)) =>
        Solving(assignment)
      case (s, _) =>
        s
    }
}
