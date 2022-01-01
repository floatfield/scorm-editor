package ru.studyground

import com.raquo.laminar.api.L._
import org.scalajs.dom
import ru.studyground.State._
import ru.studyground.solver.BucketsSolve.application
import ru.studyground.solver.Bucket

import java.util.UUID

object MainApp {

  val bucketsTaskId: BucketsTaskId = BucketsTaskId(UUID.fromString(Globals.bucketsTaskId))

  val state: Var[State] = Var(Loading(bucketsTaskId))

  val observer: Observer[Command] = state.updater(Command.dispatch)

  val application: Div = div(

  )


  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app-container")
//      render(appContainer, application)
    }(unsafeWindowOwner)
  }
}

sealed abstract class State(val id: BucketsTaskId)

object State {
  final case class Loading(override val id: BucketsTaskId) extends State(id)
  final case class Solving(assignment: BucketsAssignment) extends State(assignment.id)
  final case class Done(override val id: BucketsTaskId, answers: List[Bucket]) extends State(id)
}

sealed trait Command

object Command {
  final case class Start(assignment: BucketsAssignment) extends Command
  final case class Finish(answers: List[Bucket]) extends Command

  def dispatch(state: State, c: Command): State = (state, c) match {
    case (Loading(_), Start(assignment)) =>
      Solving(assignment)
    case (Solving(a), Finish(answers)) =>
      Done(a.id, answers)
    case (s, _) =>
      s
  }
}