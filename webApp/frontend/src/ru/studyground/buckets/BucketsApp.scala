package ru.studyground.buckets

import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import ru.studyground.BucketsPage.{ListBucketTasks, UpdateBucketTask, NewBucketTask}
import ru.studyground.buckets.BucketTaskList.BucketsList
import ru.studyground.buckets.uuidPathSegmentImplicits._
import ru.studyground._
import urldsl.errors.ErrorFromThrowable
import urldsl.vocabulary.{FromString, Printer}

import java.util.UUID
import scala.util.Try

object BucketsApp {
  val MenuTitle = "Категории"

  private val bucketsRoute =
    Route.static(ListBucketTasks, root / "buckets" / endOfSegments)
  private val newBucketRoute =
    Route.static(NewBucketTask, root / "buckets" / "new" / endOfSegments)
  private val editBucketRoute = Route[UpdateBucketTask, UUID](
    encode = page => page.id.value,
    decode = id => UpdateBucketTask(BucketsTaskId(id)),
    pattern = root / "buckets" / segment[UUID] / endOfSegments
  )

  val routes = List(bucketsRoute, newBucketRoute, editBucketRoute)

  def render(
      $page: Signal[BucketsPage],
      router: Router[Page],
      $state: Signal[MainAppState],
      commandObserver: Observer[Command]
  ): Div = {
    val splitter = SplitRender[BucketsPage, HtmlElement]($page)
      .collectStatic(ListBucketTasks)(
        BucketsList(router, $state.map(_.bucketsTaskList), commandObserver)
      )
      .collectStatic(NewBucketTask)(
        EditBucketTask.render(router, $state, Signal.fromValue(None), commandObserver)
      )
      .collectSignal[UpdateBucketTask] { $page =>
        val $id = $page.map {
          case UpdateBucketTask(id) => Some(id)
          case _ => None
        }
        EditBucketTask.render(router, $state, $id, commandObserver)
      }

    div(
      child <-- splitter.$view
    )
  }
}

object uuidPathSegmentImplicits {
  implicit def uuidFromString[A](implicit
      fromThrowable: ErrorFromThrowable[A]
  ): FromString[UUID, A] =
    FromString.factory(s =>
      Try(UUID.fromString(s)).fold[Either[A, UUID]](
        err => Left(fromThrowable.fromThrowable(err)),
        uuid => Right(uuid)
      )
    )

  implicit def uuidPrinter: Printer[UUID] = Printer.factory(_.toString)
}
