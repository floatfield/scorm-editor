package ru.studyground.buckets

import zio.json._

final case class BucketTaskId(value: Long) extends AnyVal

object BucketTaskId {
  implicit val jsonCodec: JsonCodec[BucketTaskId] = DeriveJsonCodec.gen[BucketTaskId]
}