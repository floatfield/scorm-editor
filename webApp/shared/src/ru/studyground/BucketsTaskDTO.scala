package ru.studyground

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class BucketsTaskDTO(
    name: String,
    description: String,
    maxItemsNumber: Int,
    task: String
)

object BucketsTaskDTO {
  implicit val newBucketsTaskDTOCodec: JsonCodec[BucketsTaskDTO] =
    DeriveJsonCodec.gen[BucketsTaskDTO]
}

final case class BucketsAssignment(
    id: BucketsTaskId,
    description: String,
    bucketNames: List[String],
    assignedBucketNames: List[String],
    values: List[String]
)

object BucketsAssignment {
  implicit val bucketsAssignmentEncoder: JsonCodec[BucketsAssignment] =
    DeriveJsonCodec.gen[BucketsAssignment]
}

final case class BucketDTO(name: String, values: List[String])

object BucketDTO {
  val empty: BucketDTO = BucketDTO("", Nil)
  def fromName(name: String): BucketDTO = BucketDTO(name, List.empty)

  implicit val bucketDTOCodec: JsonCodec[BucketDTO] =
    DeriveJsonCodec.gen[BucketDTO]
}

final case class AnswerDTO(
    id: BucketsTaskId,
    buckets: List[BucketDTO],
    values: List[String]
)

object AnswerDTO {
  implicit val answerCodec: JsonCodec[AnswerDTO] =
    DeriveJsonCodec.gen[AnswerDTO]
}

final case class AssessmentResult(isCorrect: Boolean) extends AnyVal

object AssessmentResult {
  implicit val assessmentResultCodec: JsonCodec[AssessmentResult] =
    DeriveJsonCodec.gen[AssessmentResult]
}
