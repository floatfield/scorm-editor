package ru.studyground

import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import org.scalajs.dom.console
import ru.studyground.Page.Login
import zio.json._

sealed trait FormState

object FormState {
  case object Editing extends FormState
  case object Loading extends FormState
  final case class Error(msg: String) extends FormState
}

object LoginApp {

  val MenuTitle = "Войти"

  val route: Route[_ <: Page, _] =
    Route.static(Login, root / "login" / endOfSegments)

  def renderLogin(
      $state: Signal[LoginState],
      mainAppObserver: Observer[Command]
  ): Div = {

    val $formState = Var[FormState](FormState.Editing)

    div(
      cls("ui four column centered grid"),
      div(
        cls("column"),
        form(
          cls("ui form"),
          cls <-- $formState.signal.map(fs =>
            List(
              "loading" -> (fs == FormState.Loading),
              "error" -> fs.isInstanceOf[FormState.Error]
            )
          ),
          child <-- $formState.signal.map {
            case FormState.Error(msg) => errorMessage(msg, $formState.writer)
            case _                    => emptyNode
          },
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
                $click --> $formState.writer.contramap[LoginState](_ =>
                  FormState.Loading
                ),
                $req --> $formState.writer.contramap[Either[String, String]](
                  _.fold[FormState](
                    err => FormState.Error(err),
                    _ => FormState.Editing
                  )
                ),
                $req --> mainAppObserver.contracollect[Either[String, String]] {
                  case Right(token) => Command.SetToken(token)
                }
              )
            }
          )
        )
      )
    )
  }

  private def errorMessage(
      msg: String,
      formObserver: Observer[FormState]
  ): Div = {
    div(
      cls("ui error message fadeout"),
      i(
        cls("close icon"),
        onClick.mapTo(FormState.Editing) --> formObserver
      ),
      div(cls("header"), "Ошибка"),
      p(msg),
      onAnimationEnd.mapTo(FormState.Editing) --> formObserver
    )
  }

}
