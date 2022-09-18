package ru.studyground.solver

import com.raquo.laminar.api.L._
import org.scalajs.dom.{DragEvent, console}
import ru.studyground.{
  AssessmentResult,
  BucketsAssignment,
  Command => MainAppCommand
}
import io.laminext.syntax.core._
import ru.studyground.solver.AssignmentState.{Done, SendingAnswer, Solving}
import ru.studyground.solver.Command.{DisplayResult, SubmitResult}

object BucketsSolve {

  def application(
      assignment: BucketsAssignment
  ): Div = {

    val state = Var(State.fromBucketsAssignment(assignment))

    val observer = state.updater(Command.dispatch)

    div(
      div(
        cls("ui equal width left aligned grid container"),
        div(
          cls("row"),
          div(
            cls("ui blue segment assignment-description"),
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
                onDragStart --> Observer[DragEvent](onNext = (e: DragEvent) => e.dataTransfer.setData("text", value)),
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
            child <-- state.signal.map { state =>
              state.assignmentState match {
                case SendingAnswer =>
                  div(cls("ui right floated loading button"))
                case Done(result) if result.isCorrect =>
                  div(
                    cls("ui right floated labeled button"),
                    div(
                      cls("ui green button"),
                      i(cls("check circle icon"))
                    ),
                    div(cls("ui basic green left pointing label"), "Правильно")
                  )
                case Done(_) =>
                  div(
                    cls("ui right floated labeled button"),
                    div(
                      cls("ui red button"),
                      i(cls("ui times circle icon"))
                    ),
                    div(cls("ui basic red left pointing label"), "Неправильно")
                  )
                case Solving =>
                  div(
                    cls("ui right floated blue button"),
                    "Готово",
                    thisEvents(onClick) -->
                      observer.contramap[Any](_ => SubmitResult)
                  )
              }
            }
          )
        )
      ),
      Modal(
        state.signal.map(_.modal),
        state.signal.map(_.values),
        observer
      ),
      state.signal
        .flatMap {
          case state if state.assignmentState == SendingAnswer =>
            assesReq(state.id, state.answers.toList, state.values)
          case _ =>
            EventStream.empty
        }
        .flatMap {
          case Right(result) => EventStream.fromValue(result, emitOnce = true)
          case Left(err) =>
            console.error(err)
            EventStream.empty
        } --> observer.contramap[AssessmentResult](DisplayResult)
    )
  }
}
