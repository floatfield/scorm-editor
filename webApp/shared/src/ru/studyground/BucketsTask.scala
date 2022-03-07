package ru.studyground

import zio.json.JsonCodec.{
  seq => seqCodec,
  string => stringCodec,
  uuid => uuidCodec
}
import zio.json._

import java.util.UUID

final case class BucketName(name: String) extends AnyVal

object BucketName {
  implicit val bucketNameCodec: JsonCodec[BucketName] = stringCodec.xmap(
    BucketName(_),
    bucketName => bucketName.name
  )
}

final case class BucketNames(names: Seq[BucketName]) {
  def +(that: BucketNames): BucketNames = BucketNames(names ++ that.names)
  def map(f: String => String): BucketNames =
    BucketNames(names.map(name => BucketName(f(name.name))))
  override def toString: String = names.map(n => s"- ${n.name}").mkString("\n")
}

object BucketNames {
  val empty: BucketNames = BucketNames(Nil)

  implicit val bucketNamesCodec: JsonCodec[BucketNames] = seqCodec[String].xmap(
    names => BucketNames(names.map(BucketName(_))),
    _.names.map(_.name)
  )
}

final case class FullBucket(bucketName: BucketName, values: Seq[String]) {
  self =>
  def mapValues(f: String => String): FullBucket =
    self.copy(values = values.map(f))
  override def toString = s"- ${bucketName.name}\n${values.mkString("\n")}"
}

object FullBucket {
  implicit val fullBucketCodec: JsonCodec[FullBucket] =
    DeriveJsonCodec.gen[FullBucket]
}

final case class ParsedBucketsTask(
    names: BucketNames,
    fullBuckets: Seq[FullBucket]
) {
  override def toString: String =
    s"$names\n${fullBuckets.mkString("\n")}"
}

final case class BucketsTaskId(value: UUID) extends AnyVal

object BucketsTaskId {
  implicit val bucketsTaskIdCodec: JsonCodec[BucketsTaskId] = uuidCodec.xmap(
    BucketsTaskId(_),
    _.value
  )
}

final case class BucketsTask(
    id: BucketsTaskId,
    name: String,
    description: String,
    maxItemsNumber: Int,
    bucketNames: BucketNames,
    fullBuckets: Seq[FullBucket]
) {
  val bucketsTaskDTO: BucketsTaskDTO =
    BucketsTaskDTO(
      name = name,
      description = description,
      maxItemsNumber = maxItemsNumber,
      task =
        fullBuckets.map(_.toString).mkString("\n") + "\n" + bucketNames.toString
    )
}

final case class ParseError(msg: String) extends Exception

object BucketsTask {
  implicit val bucketsTaskCodec: JsonCodec[BucketsTask] =
    DeriveJsonCodec.gen[BucketsTask]
}

