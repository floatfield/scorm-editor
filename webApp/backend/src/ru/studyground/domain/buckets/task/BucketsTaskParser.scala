package ru.studyground.domain.buckets.task

import ru.studyground.{BucketName, BucketNames, BucketsTask, BucketsTaskId, FullBucket, ParsedBucketsTask}
import ru.studyground.BucketsTaskDTO
import fastparse.NoWhitespace._
import fastparse._
import xml.Utility.escape

final case class ParseError(msg: String) extends Exception

object BucketsTaskParser {
  def parseBucketsTask(t: BucketsTaskDTO, id: BucketsTaskId): Either[ParseError, BucketsTask] =
    parse(t.task, Parser.bucketsTask(_)).fold(
      (_, _, _) => Left(ParseError(s"Неправильный формат файла с заданиями")),
      (x, _) =>
        Right(
          BucketsTask(
            id,
            escape(t.name),
            escape(t.description),
            t.maxItemsNumber,
            x.names.map(escape),
            x.fullBuckets.map(_.mapValues(escape))
          )
        )
    )
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