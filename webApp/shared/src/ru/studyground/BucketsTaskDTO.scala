package ru.studyground

import zio.json.{DeriveJsonCodec, DeriveJsonEncoder, JsonCodec, JsonEncoder}

final case class BucketsTaskDTO(
    name: String,
    description: String,
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
  implicit val bucketsAssignmentEncoder: JsonEncoder[BucketsAssignment] =
    DeriveJsonEncoder.gen[BucketsAssignment]
}
