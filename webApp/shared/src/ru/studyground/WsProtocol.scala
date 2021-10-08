package ru.studyground

import zio.json._

sealed trait ClientMessage

final case class Token(token: String) extends ClientMessage
final case class RemoveTask(bucketsTaskId: BucketsTaskId) extends ClientMessage

object Token {
  implicit val tokenCodec: JsonCodec[Token] = DeriveJsonCodec.gen
}

object RemoveTask {
  implicit val removeTaskCodec: JsonCodec[RemoveTask] = DeriveJsonCodec.gen
}

object ClientMessage {
  implicit val clientMessageCodec: JsonCodec[ClientMessage] =
    DeriveJsonCodec.gen
}

sealed trait ServerMessage

final case class AddBucketsTasks(addedBucketsTasks: List[BucketsTask])
    extends ServerMessage
final case class RemoveBucketsTasks(removedIds: List[BucketsTaskId])
    extends ServerMessage
final case class UpdateBucketsTask(updatedTask: BucketsTask)
  extends ServerMessage

object AddBucketsTasks {
  implicit val addBucketsTaskCodec: JsonCodec[AddBucketsTasks] =
    DeriveJsonCodec.gen
}

object RemoveBucketsTasks {
  implicit val removeBucketsTaskCodec: JsonCodec[RemoveBucketsTasks] =
    DeriveJsonCodec.gen
}

object UpdateBucketsTask {
  implicit val updateBucketsTask: JsonCodec[UpdateBucketsTask] =
    DeriveJsonCodec.gen
}

object ServerMessage {
  implicit val serverMessageCodec: JsonCodec[ServerMessage] =
    DeriveJsonCodec.gen
}
