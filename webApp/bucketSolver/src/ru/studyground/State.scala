package ru.studyground

case class Bucket(name: String, values: List[String])

object Bucket {
  val empty: Bucket = Bucket("", Nil)
}

case class Task(
    description: String,
    buckets: List[Bucket],
    bucketNames: List[String]
) {
  def toState: State = {
    val values =
      buckets
        .foldLeft(Set.empty[String])((bs, b) => bs ++ b.values)
        .toList
        .sorted
    val assignedBucketNames = (buckets.map(_.name).toSet -- bucketNames).toList
    val answers =
      assignedBucketNames.map(name => Bucket(name, Nil)).toVector ++
        List.fill(buckets.size - assignedBucketNames.size)(Bucket.empty)
    State(
      answers = answers,
      values = values,
      bucketNames = bucketNames.toSet,
      modal = ModalState.Closed
    )
  }
}

case class State(
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
      answers.updated(bucketId, bucket.copy(values = newAnswers))
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
