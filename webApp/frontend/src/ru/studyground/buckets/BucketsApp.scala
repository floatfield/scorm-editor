package ru.studyground.buckets

import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import BucketTasksList.BucketsList
import ru.studyground.{BucketsPage, Page}
import ru.studyground.BucketsPage.{NewBucketTask => NewBucketTaskPage, ListBucketTasks, EditBucketTask}

object BucketsApp {
  val MenuTitle = "Категории"

  private val bucketsRoute =
    Route.static(ListBucketTasks, root / "buckets" / endOfSegments)
  private val newBucketRoute =
    Route.static(NewBucketTaskPage, root / "buckets" / "new" / endOfSegments)
  private val editBucketRoute = Route[EditBucketTask, Long](
    encode = page => page.id.value,
    decode = id => EditBucketTask(BucketTaskId(id)),
    pattern = root / "buckets" / segment[Long] / endOfSegments
  )

  val routes = List(bucketsRoute, newBucketRoute, editBucketRoute)

  def render(
      $page: Signal[BucketsPage],
      router: Router[Page]
  ): Div = {
    val splitter = SplitRender[BucketsPage, HtmlElement]($page)
      .collectStatic(ListBucketTasks)(
        BucketsList(router)
      )
      .collectStatic(NewBucketTaskPage)(
        NewBucketTask.render()
      )

    div(
      child <-- splitter.$view
    )
  }
}
