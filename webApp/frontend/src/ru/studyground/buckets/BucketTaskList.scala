package ru.studyground.buckets

import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import org.scalajs.dom.MouseEvent
import ru.studyground.BucketsListCommand.{RemoveBucketsTasks, SetBucketsTasks}
import ru.studyground.BucketsPage.UpdateBucketTask
import ru.studyground._
import ru.studyground.buckets.Requests.{loadBucketTasks, removeBucketsTask}
import io.laminext.syntax.core._

object BucketTaskList {
  def BucketsList(
      router: Router[Page],
      $bucketTaskList: Signal[List[BucketsTask]],
      commandObserver: Observer[Command]
  ): Div =
    div(
      loadBucketTasks --> commandObserver.contracollect[Either[String, List[BucketsTask]]] {
        case Right(tasks) =>
          SetBucketsTasks(tasks)
      },
      cls("ui loading container"),
      cls("buckets-tasks-list"),
      h3("Категории"),
      div(
        cls("ui divided selection list"),
        children <-- $bucketTaskList.split(_.id) {
          case (key, _, bucketTaskSignal) =>
            div(
              cls("item"),
              div(
                cls("right floated content"),
                a(
                  cls("ui icon button"),
                  i(cls("eye icon")),
                  href(s"buckets/${key.value}"),
                  thisEvents(onClick.stopPropagation) --> Observer.empty
                ),
                div(
                  cls("ui icon button"),
                  i(cls("download icon"))
                ),
                div(
                  cls("ui icon button"),
                  i(cls("trash alternate icon")),
                  thisEvents(onClick.preventDefault.stopPropagation)
                    .flatMap(_ => removeBucketsTask(key)) -->
                      commandObserver.contracollect[Option[String]]{
                        case None => RemoveBucketsTasks(List(key))
                      }
                )
              ),
              div(
                cls("content"),
                div(cls("header"), child <-- bucketTaskSignal.map(_.name)),
                child <-- bucketTaskSignal.map(_.description)
              ),
              onClick.preventDefault --> Observer[Any](_ => router.pushState(UpdateBucketTask(key)))
            )
        }
      ),
      div(
        cls("ui blue left labeled icon button"),
        i(cls("plus icon")),
        "Добавить задание",
        onClick --> Observer[MouseEvent](_ =>
          router.pushState(BucketsPage.NewBucketTask)
        )
      )
    )
}
