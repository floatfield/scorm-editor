package ru.studyground

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L._

import scala.scalajs.js

@js.native
trait JsDropdown extends js.Object {
  def dropdown(config: js.Dictionary[js.Any]): JsDropdown = js.native
}

object Dropdown {
  def apply(
      defaultText: String,
      options: Signal[List[String]],
      onChange: Option[String] => Unit,
      text: Signal[Option[String]],
      parentId: String
  ): Div = {
    def change: js.Function3[String, js.UndefOr[String], js.Any, Unit] =
      (_, mbText, _) => onChange(mbText.toOption)

    div(
      cls("ui tiny selection dropdown"),
      onMountCallback(_ =>
        JQuery(s"#$parentId .ui.dropdown")
          .dropdown(
            js.Dictionary(
              "clearable" -> true,
              "onChange" -> change
            )
          )
      ),
      child <-- text.map(
        _.fold[Node](i(cls("dropdown icon")))(_ =>
          i(cls("dropdown icon clear"))
        )
      ),
      child <-- text.map(_.fold[Node](emptyNode)(t => div(cls("text"), t))),
      child <-- text.map(
        _.fold[Node](div(cls("default text"), defaultText))(_ => emptyNode)
      ),
      div(
        cls("menu"),
        children <-- options.map(
          _.map(item =>
            div(
              cls("item"),
              customProp("data-value", StringAsIsCodec)(item),
              item
            )
          )
        )
      )
    )
  }
}
