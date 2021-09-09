package ru.studyground

import ru.studyground.buckets.BucketTaskId
import zio.json.{DeriveJsonCodec, JsonCodec}

sealed trait Page
sealed trait BucketsPage extends Page
sealed trait QuizPage extends Page

object Page {
  case object Welcome extends Page
  case object Login extends Page

  implicit val pageCodec: JsonCodec[Page] = DeriveJsonCodec.gen[Page]
}

object BucketsPage {
  case object ListBucketTasks extends BucketsPage
  case object NewBucketTask extends BucketsPage
  case class EditBucketTask(id: BucketTaskId) extends BucketsPage
}