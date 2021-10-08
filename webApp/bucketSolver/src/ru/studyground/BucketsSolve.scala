package ru.studyground

import com.raquo.laminar.api.L._
import org.scalajs.dom.DragEvent
import ru.studyground.Command._


sealed trait Command

object Command {
  case class SelectBucketName(id: Int, name: String) extends Command
  case class ClearBucketName(id: Int) extends Command
  case class AddAnswer(bucketId: Int, answer: String) extends Command
  case class RemoveAnswer(bucketId: Int, answer: String) extends Command
  case class SetAnswers(bucketId: Int, answers: List[String]) extends Command
  case class ShowModal(bucketId: Int) extends Command
  case object ResetModal extends Command
}

object BucketsSolve {

  val longBoi =
    "The REST API is the fundamental fabric of Kubernetes. All operations and communications between components, and external user commands are REST API calls that the API Server handles. Consequently, everything in the Kubernetes platform is treated as an API object and has a corresponding entry in the API."

  val task = Task(
    description =
      "Certain resources and API groups are enabled by default. You can enable or disable them by setting --runtime-config on the API server. The --runtime-config flag accepts comma separated <key>[=<value>] pairs describing the runtime configuration of the API server. If the =<value> part is omitted, it is treated as if =true is specified.",
    buckets = List(
      Bucket(
        "one",
        List("Bucket 1 text goes here", "Some bucket text", longBoi)
      ),
      Bucket(
        "two",
        List("Bucket 2 text goes here", "Some bucket text", longBoi)
      ),
      Bucket(
        "All operations and communications between components, and external user commands are REST API calls that the API Server handles",
        List("Bucket 3 text goes here", longBoi)
      )
    ),
    bucketNames = List("two", "three")
  )

  private def dispatch(state: State, command: Command): State =
    command match {
      case SelectBucketName(id, name) =>
        state.assignBucketName(id, name)
      case ClearBucketName(id) =>
        state.assignBucketName(id, "")
      case AddAnswer(bucketId, answerId) =>
        state.addAnswer(bucketId, answerId)
      case RemoveAnswer(bucketId, answer) =>
        state.removeAnswer(bucketId, answer)
      case SetAnswers(bucketId, answerIds) =>
        state
          .setAnswers(bucketId, answerIds)
          .resetModal
      case ShowModal(bucketId) =>
        state.showModal(bucketId)
      case ResetModal =>
        state.resetModal
    }

  val stateVar = Var(task.toState)

  val commandObserver = stateVar.updater[Command](dispatch)

  val application = div(
    div(
      cls("ui equal width left aligned grid container"),
      div(
        cls("row"),
        div(
          cls("ui blue segment"),
          task.description
        )
      ),
      div(
        cls("row"),
        div(
          cls("computer only column"),
          stateVar.signal
            .now()
            .values
            .map(value =>
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
          children <-- stateVar.signal.map(_.answers.zipWithIndex).split(_._2) {
            case (id, _, sig) =>
              AnswerBucket.renderBucket(
                id,
                stateVar.signal.map(_.bucketNames.toList.sorted),
                sig.map(_._1),
                commandObserver
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
            "Done"
          )
        )
      )
    ),
    Modal(
      stateVar.signal.map(_.modal),
      stateVar.signal.map(_.values),
      commandObserver
    )
  )
}
