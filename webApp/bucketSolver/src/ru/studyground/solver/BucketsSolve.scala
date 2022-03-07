package ru.studyground.solver

import com.raquo.laminar.api.L._
import org.scalajs.dom.DragEvent
import ru.studyground.BucketsAssignment
import ru.studyground.{Command => MainAppCommand}
import io.laminext.syntax.core._

object BucketsSolve {

  def application(assignment: BucketsAssignment, mainAppObserver: Observer[MainAppCommand]): Div = {

    val state = Var(State.fromBucketsAssignment(assignment))

    val observer = state.updater(Command.dispatch)

    div(
      div(
        cls("ui equal width left aligned grid container"),
        div(
          cls("row"),
          div(
            cls("ui blue segment"),
            assignment.description
          )
        ),
        div(
          cls("row"),
          div(
            cls("computer only column"),
            assignment.values.map(value =>
              div(
                cls("ui segment"),
                draggable(true),
                onDragStart --> Observer[DragEvent](onNext =
                  (e: DragEvent) => e.dataTransfer.setData("text", value)
                ),
                value
              )
            )
          ),
          div(
            cls("column dropzone"),
            children <-- state.signal.map(_.answers.zipWithIndex).split(_._2) {
              case (id, _, sig) =>
                AnswerBucket.renderBucket(
                  id,
                  state.signal.map(_.bucketNames.toList.sorted),
                  sig.map(_._1),
                  observer
                )
            }
          )
        ),
        div(
          cls("row"),
          div(
            cls("column"),
            div(
              cls("ui right floated blue button"),
              "Done",
              thisEvents(onClick).withCurrentValueOf(state.signal) -->
                mainAppObserver.contramap[(Any, State)] {
                  case (_, state) =>  MainAppCommand.Finish(state.answers.toList)
                }
            )
          )
        )
      ),
      Modal(
        state.signal.map(_.modal),
        state.signal.map(_.values),
        observer
      )
    )
  }
}
