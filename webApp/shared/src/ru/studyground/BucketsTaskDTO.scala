package ru.studyground

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class BucketsTaskDTO(
    name: String,
    description: String,
    task: String
)

object BucketsTaskDTO {
  implicit val newBucketsTaskDTOCodec: JsonCodec[BucketsTaskDTO] =
    DeriveJsonCodec.gen[BucketsTaskDTO]
}
