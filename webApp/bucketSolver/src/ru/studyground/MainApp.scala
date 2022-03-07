package ru.studyground

import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalajs.dom.console
import ru.studyground.Command.Start
import ru.studyground.State._
import ru.studyground.solver.BucketsSolve.{application => inProgress}
import ru.studyground.{BucketDTO => Bucket}
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

  def assesReq(
      id: BucketsTaskId,
      answers: List[Bucket],
      values: List[String]
  ): EventStream[Either[String, AssessmentResult]] =
    AjaxEventStream
      .post(
        url = s"$bucketsServer/assignments/asses",
        headers = Map("Content-Type" -> "application/json"),
        data = AnswerDTO(id, answers, values).toJson
      )
      .map(r => r.responseText.fromJson[AssessmentResult])
      .recover {
        case err: AjaxStreamError =>
          console.error(err.getMessage)
          Some(Left(err.xhr.responseText))
      }

  val state: Var[State] = Var(Loading(bucketsTaskId))

  val observer: Observer[Command] = state.updater(Command.dispatch)

  val loading: Div = div("loading")

  val done: Div = div("done")

  val application: Div = div(
    state.signal --> Observer[State] {
      case Done(id, answers) => println(id, answers)
      case _                 => ()
    },
    loadAssignmentReq --> observer
      .contracollect[Either[String, BucketsAssignment]] {
        case Right(assignment) => Start(assignment)
      },
    child <-- state.signal.map {
      case Loading(_)          => loading
      case Solving(assignment) => inProgress(assignment, observer)
      case Done(_, _)          => done
    }
  )

  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app-container")
      render(appContainer, application)
    }(unsafeWindowOwner)
  }
}

sealed abstract class State(val id: BucketsTaskId)

object State {
  final case class Loading(override val id: BucketsTaskId) extends State(id)
  final case class Solving(assignment: BucketsAssignment)
      extends State(assignment.id)
  final case class Done(override val id: BucketsTaskId, answers: List[Bucket])
      extends State(id)
}

sealed trait Command

object Command {
  final case class Start(assignment: BucketsAssignment) extends Command
  final case class Finish(answers: List[Bucket]) extends Command

  def dispatch(state: State, c: Command): State =
    (state, c) match {
      case (Loading(_), Start(assignment)) =>
        Solving(assignment)
      case (Solving(a), Finish(answers)) =>
        Done(a.id, answers)
      case (s, _) =>
        s
    }
}
