package ru.studyground.solver

import ru.studyground.AssessmentResult

sealed trait Command

object Command {
  case class SelectBucketName(id: Int, name: String) extends Command
  case class ClearBucketName(id: Int) extends Command
  case class AddAnswer(bucketId: Int, answer: String) extends Command
  case class RemoveAnswer(bucketId: Int, answer: String) extends Command
  case class SetAnswers(bucketId: Int, answers: List[String]) extends Command
  case class ShowModal(bucketId: Int) extends Command
  case object ResetModal extends Command
  case object SubmitResult extends Command
  case class DisplayResult(assessmentResult: AssessmentResult) extends Command

  def dispatch(state: State, command: Command): State =
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
      case SubmitResult =>
        state.submitResult
      case DisplayResult(result) =>
        state.displayResult(result)
    }
}

