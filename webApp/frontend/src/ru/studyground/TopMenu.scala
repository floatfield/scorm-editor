package ru.studyground

import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint._
import org.scalajs.dom.{console, html}
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

  def logoutButton(implicit router: Router[Page]): Div =
    div(
      cls("item"),
      a(
        cls("ui button"),
        "Выйти",
        inContext { thisNode =>
          val $req = thisNode.events(onClick.preventDefault).flatMap { _ =>
            val headers = Map("Content-type" -> "application/json")
            val url = "/logout"
            AjaxEventStream
              .post(url, "", headers = headers)
              .map(r => Right(r.responseText))
              .recover {
                case err: AjaxStreamError =>
                  console.error(err.getMessage)
                  Some(Left(err.xhr.responseText))
              }
          }
          List(
            $req --> Observer[Either[String, String]](_.foreach(_ => router.pushState(Login)))
          )
        }
      )
    )

  def menu(implicit router: Router[Page]): Div =
    div(
      cls("ui container"),
      menuItem(Welcome, WelcomeApp.MenuTitle),
      menuItem(BucketsPage.ListBucketTasks, BucketsApp.MenuTitle),
      div(
        cls("right menu"),
        logoutButton
      )
    )

  def apply(state: Signal[MainAppState], observer: Observer[Command])(
      implicit router: Router[Page]
  ): ReactiveHtmlElement[html.Div] = {
    div(
      cls("ui large top menu"),
      child <-- router.$currentPage.map {
        case Login => emptyNode
        case _ => menu
      }
    )
  }
}
