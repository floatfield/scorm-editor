package ru.studyground

import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import org.scalajs.dom.console
import ru.studyground.Page.Login
import ru.studyground.common.Form
import zio.json._

object LoginApp {

  val MenuTitle = "Войти"

  val route: Route[_ <: Page, _] =
    Route.static(Login, root / "login" / endOfSegments)

  def renderLogin(
      $state: Signal[LoginState],
      mainAppObserver: Observer[Command],
      router: Router[Page]
  ): Div = {
    val formContent = div(
      div(
        cls("field"),
        input(
          typ("text"),
          name("login"),
          placeholder("логин"),
          value <-- $state.map(_.name),
          onInput.preventDefault.mapToValue --> mainAppObserver
            .contramap[String](LoginCommand.SetName)
        )
      ),
      div(
        cls("field"),
        input(
          typ("password"),
          name("password"),
          placeholder("пароль"),
          value <-- $state.map(_.password),
          onInput.preventDefault.mapToValue --> mainAppObserver
            .contramap[String](LoginCommand.SetPassword)
        )
      ),
      button(
        cls("ui primary button"),
        "Войти",
        inContext { thisNode =>
          val $click =
            thisNode.events(onClick.preventDefault).sample($state)
          val $req = $click.flatMap {
            s =>
              val headers = Map("Content-type" -> "application/json")
              val url = "/login"
              AjaxEventStream
                .post(url, s.loginDTO.toJson, headers = headers)
                .map(r => Right(r.responseText))
                .recover {
                  case err: AjaxStreamError =>
                    console.error(err.getMessage)
                    Some(Left(err.xhr.responseText))
                }

          }
          List(
            $click --> mainAppObserver.contramap[LoginState](_ =>
              LoginCommand.SetFormState(FormState.Loading)
            ),
            $req --> mainAppObserver.contramap[Either[String, String]](
              _.fold[LoginCommand](
                err => LoginCommand.SetFormState(FormState.Error(err)),
                _ => LoginCommand.SetFormState(FormState.Editing)
              )
            ),
            $req --> Observer[Either[String, String]](_.foreach(_ => router.pushState(Page.Welcome)))
          )
        }
      )
    )
    Form(
      formContent,
      $state.map(_.formState),
      mainAppObserver.contramap[Unit](_ => LoginCommand.SetFormState(FormState.Editing))
    )
  }
}
