package ru.studyground.http

import ru.studyground.{AnswerDTO, AssessmentResult, BucketDTO, BucketsAssignment, BucketsTask, FullBucket}
import ru.studyground.domain.buckets.task.BucketsTaskRepository
import zhttp.http.Http.collectM
import zhttp.http._
import zio.{Has, URIO, ZIO}
import zio.blocking.Blocking
import zio.random.Random
import zio.json._

import java.util.UUID

object Assignments {

  type Env = Random with Has[BucketsTaskRepository] with Blocking

  val routes: Http[Env, HttpError, Request, UResponse] =
    jsonContent[Env] >>> collectM[Request] {
      case Method.GET -> !! / "assignments" / uuid =>
        ZIO
          .effect(UUID.fromString(uuid))
          .orElseFail(HttpError.BadRequest(s"Malformed uuid: $uuid"))
          .flatMap(getAssignment)
      case r @ Method.POST -> !! / "assignments" / "assess" =>
        assess(r)

    }

  private def getAssignment(uuid: UUID): ZIO[Env, HttpError, UResponse] =
    for {
      bucketsTask <-
        BucketsTaskRepository
          .get(uuid)
          .flatMap(ZIO.fromOption(_))
          .orElseFail(
            HttpError.NotFound(Path.End / "assignments" / uuid.toString)
          )
      assignment <- toAssignment(bucketsTask)
    } yield Response.jsonString(assignment.toJson)

  private def toAssignment(task: BucketsTask): URIO[Random, BucketsAssignment] =
    ZIO.foreach(task.fullBuckets)(pickItems(_, task.maxItemsNumber)).map {
      itemsList =>
        val items = itemsList.flatten.toList
        val bucketNames = task.fullBuckets.map(_.bucketName.name).toList
        val names = task.bucketNames.names.map(_.name).toList
        BucketsAssignment(
          id = task.id,
          description = task.description,
          bucketNames = names,
          assignedBucketNames = (bucketNames.toSet -- names).toList,
          values = items.distinct
        )
    }

  private def pickItems(
      fullBucket: FullBucket,
      maxItemsNumber: Int
  ): URIO[Random, List[String]] =
    for {
      n <- zio.random.nextIntBetween(
        1,
        Math.min(fullBucket.values.length, maxItemsNumber + 1)
      )
      items <- zio.random.shuffle(fullBucket.values.toList).map(_.take(n))
    } yield items

  private def assess(
      r: Request
  ): ZIO[Has[BucketsTaskRepository] with Blocking, HttpError, UResponse] =
    for {
      answerDTO <- extractData[AnswerDTO](r)
      mbTask <-
        BucketsTaskRepository
          .get(answerDTO.id.value)
          .orElseFail(HttpError.InternalServerError())
      bucketsTask <-
        ZIO
          .fromOption(mbTask)
          .orElseFail(
            HttpError.BadRequest(
              s"Bucket task with id ${answerDTO.id.value} not found"
            )
          )
      isCorrect = validateAnswer(answerDTO, bucketsTask.fullBuckets)
    } yield Response.jsonString(AssessmentResult(isCorrect).toJson)

  private def validateAnswer(
      answerDTO: AnswerDTO,
      fullBuckets: Seq[FullBucket]
  ): Boolean = {
    val values = answerDTO.values.toSet
    answerDTO.buckets.forall {
      case BucketDTO(name, answerValues) =>
        fullBuckets.find(_.bucketName.name == name).exists {
          case FullBucket(_, allValues) =>
            values.intersect(allValues.toSet) == answerValues.toSet
        }
    }
  }

}
