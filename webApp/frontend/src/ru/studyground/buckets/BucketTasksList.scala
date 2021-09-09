package ru.studyground.buckets

import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import org.scalajs.dom.MouseEvent
import ru.studyground.{BucketsPage, Page}

object BucketTasksList {
  def BucketsList(router: Router[Page]): Div = div(
    cls("eight wide column"),
    h3("Категории"),
    div(
      cls("ui blue left labeled icon button"),
      i(cls("plus icon")),
      "Добавить задание",
      onClick --> Observer[MouseEvent](_ => router.pushState(BucketsPage.NewBucketTask))
    ),
    p("Создавайте категории желтые коробочки бесплатно")
  )
}
