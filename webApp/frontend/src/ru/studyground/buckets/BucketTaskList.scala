package ru.studyground.buckets

import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import io.laminext.websocket.WebSocket
import org.scalajs.dom.MouseEvent
import ru.studyground.BucketsPage.UpdateBucketTask
import ru.studyground._

object BucketTaskList {
  def BucketsList(
      router: Router[Page],
      $bucketTaskList: Signal[List[BucketsTask]],
      ws: WebSocket[ServerMessage, ClientMessage]
  ): Div =
    div(
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
                div(
                  cls("ui icon button"),
                  i(cls("download icon"))
                ),
                div(
                  cls("ui icon button"),
                  i(cls("trash alternate icon")),
                  onClick.preventDefault.stopPropagation --> ws.send.contramap[Any](_ => RemoveTask(key))
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
