package ru.studyground

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint._
import org.scalajs.dom.{MouseEvent, html}
import ru.studyground.Page.{Login, Welcome}
import ru.studyground.buckets.BucketsApp

object TopMenu {

  def menuItem(
      page: Page,
      name: String
  )(implicit
      router: Router[Page]
  ): Anchor =
    a(
      cls("item"),
      cls <-- router.$currentPage.map(p => List("active" -> (p == page))),
      href(router.absoluteUrlForPage(page)),
      name,
      onClick.preventDefault --> (_ => router.pushState(page))
    )

  def buttonMenuItem(
      page: Page,
      name: String
  )(implicit router: Router[Page]): Div =
    div(
      cls("item"),
      a(
        cls("ui button"),
        href(router.absoluteUrlForPage(page)),
        name,
        onClick.preventDefault --> (_ => router.pushState(page))
      )
    )

  def logoutButton(observer: Observer[Command]): Div =
    div(
      cls("item"),
      a(
        cls("ui button"),
        "Выйти",
        onClick.preventDefault --> observer.contramap[MouseEvent](_ => Command.Logout)
      )
    )

  def apply(state: Signal[MainAppState], observer: Observer[Command])(
      implicit router: Router[Page]
  ): ReactiveHtmlElement[html.Div] = {
    div(
      cls("ui large top menu"),
      div(
        cls("ui container"),
        menuItem(Welcome, WelcomeApp.MenuTitle),
        menuItem(BucketsPage.ListBucketTasks, BucketsApp.MenuTitle),
//        a(cls("item"), "Тесты"),
        div(
          cls("right menu"),
          child <-- state.map(_.token match {
            case Some(_) => logoutButton(observer)
            case None => buttonMenuItem(
              Login,
              LoginApp.MenuTitle
            )
          })
        )
      )
    )
  }
}
