package ru.studyground

import fastparse.NoWhitespace._
import fastparse._
import zio.json.JsonCodec.{
  seq => seqCodec,
  string => stringCodec,
  uuid => uuidCodec
}
import zio.json._

import java.util.UUID

final case class BucketName(name: String)

object BucketName {
  implicit val bucketNameCodec: JsonCodec[BucketName] = stringCodec.xmap(
    BucketName(_),
    bucketName => bucketName.name
  )
}

final case class BucketNames(names: Seq[BucketName]) {
  def +(that: BucketNames): BucketNames = BucketNames(names ++ that.names)
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
    bucketNames: BucketNames,
    fullBuckets: Seq[FullBucket]
) {
  val bucketsTaskDTO: BucketsTaskDTO =
    BucketsTaskDTO(
      name = name,
      description = description,
      task =
        fullBuckets.map(_.toString).mkString("\n") + "\n" + bucketNames.toString
    )
}

final case class ParseError(msg: String) extends Exception

object BucketsTask {

  def fromBucketsTaskDTO(
      t: BucketsTaskDTO,
      id: BucketsTaskId
  ): Either[ParseError, BucketsTask] =
    parse(t.task, Parser.bucketsTask(_)).fold(
      (_, _, _) => Left(ParseError(s"Неправильный формат файла с заданиями")),
      (x, _) =>
        Right(
          BucketsTask(
            id,
            t.name,
            t.description,
            x.names,
            x.fullBuckets
          )
        )
    )

  implicit val bucketsTaskCodec: JsonCodec[BucketsTask] =
    DeriveJsonCodec.gen[BucketsTask]
}

object Parser {
  def bucketName[_: P]: P[BucketName] =
    P("-" ~ " ".rep ~ CharPred(_ != '\n').rep.! ~ ("\n" | End))
      .map(BucketName(_))
  def bucketNames[_: P]: P[BucketNames] =
    P((bucketName ~ &("-" | End)).rep).map(BucketNames(_))

  def bucketValue[_: P]: P[String] =
    P(&(CharPred(_ != '-')) ~ CharPred(_ != '\n').rep.! ~ ("\n" | End))
  def bucketValues[_: P]: P[Seq[String]] = P(bucketValue.rep(1))

  def fullBucket[_: P]: P[FullBucket] =
    P(bucketName ~ bucketValues).map {
      case (name, values) => FullBucket(name, values)
    }

  def bucketsTask[_: P]: P[ParsedBucketsTask] =
    P(fullBucket.rep(1) ~ bucketNames).map {
      case (fs, ns) => ParsedBucketsTask(ns, fs)
    }
}
