package ru.studyground.solver

import ru.studyground.{BucketsAssignment, BucketsTaskId, BucketDTO => Bucket}

case class State(
    id: BucketsTaskId,
    description: String,
    answers: Vector[Bucket],
    values: List[String],
    bucketNames: Set[String],
    modal: ModalState
) { self =>
  def assignBucketName(id: Int, name: String): State = {
    val bucket = answers(id)
    val updatedAnswers = answers.updated(id, bucket.copy(name = name))
    self.copy(
      answers = updatedAnswers,
      bucketNames = (bucketNames + bucket.name - name).filter(_.nonEmpty)
    )
  }

  def addAnswer(bucketId: Int, answer: String): State = {
    val bucket = answers(bucketId)
    val newAnswers = (bucket.values.toSet + answer).toList.sorted
    self.copy(
      answers = answers.updated(bucketId, bucket.copy(values = newAnswers))
    )
  }

  def removeAnswer(bucketId: Int, answer: String): State = {
    val bucket = answers(bucketId)
    self.copy(
      answers = answers.updated(
        bucketId,
        bucket.copy(values = bucket.values.filterNot(_ == answer))
      )
    )
  }

  def setAnswers(bucketId: Int, as: List[String]): State = {
    val bucket = answers(bucketId)
    self.copy(
      answers = answers.updated(bucketId, bucket.copy(values = as))
    )
  }

  def showModal(bucketId: Int): State =
    self.copy(
      modal = ModalState.Open(bucketId, answers(bucketId))
    )

  def resetModal: State =
    self.copy(modal = ModalState.Closed)
}

object State {
  def fromBucketsAssignment(assignment: BucketsAssignment): State = State(
    id = assignment.id,
    description = assignment.description,
    answers = assignment.assignedBucketNames.map(Bucket.fromName).toVector,
    values = assignment.values,
    bucketNames = assignment.bucketNames.toSet,
    modal = ModalState.Closed
  )
}
