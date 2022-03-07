package ru.studyground.solver

import ru.studyground.{BucketDTO => Bucket}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L._
import org.scalajs.dom.MouseEvent

import scala.scalajs.js

@js.native
trait JsModal extends js.Object {
  def modal(arg: js.Any): JsModal = js.native
}

sealed trait ModalState

object ModalState {
  case class Open(bucketId: Int, bucket: Bucket) extends ModalState
  case object Closed extends ModalState
}

object Modal {
  def apply(
      modalState: Signal[ModalState],
      values: Signal[List[String]],
      commandObserver: Observer[Command]
  ): Div = {
    val selectedItems = Var[List[String]](Nil)

    def setAnswers(id: Int): js.Function1[js.Any, Unit] = _ =>
      commandObserver.onNext(Command.SetAnswers(id, selectedItems.now()))

    val clearItems: js.Function1[js.Any, Unit] = _ => {
      selectedItems.set(Nil)
      commandObserver.onNext(Command.ResetModal)
    }

    modalState.foreach {
      case ModalState.Open(id, b) =>
        selectedItems.set(b.values)
        JQuery("#modal").modal(
          js.Dictionary(
            "onApprove" -> setAnswers(id),
            "onDeny" -> clearItems,
            "onHide" -> clearItems
          )
        ).modal("show")
      case ModalState.Closed =>
        selectedItems.set(Nil)
    }(unsafeWindowOwner)

    div(
      idAttr("modal"),
      cls("ui longer modal"),
      i(cls("close icon")),
      div(
        cls("header"),
        child <-- modalState.map {
          case ModalState.Closed => ""
          case ModalState.Open(_, Bucket(name, _)) =>
            s"""Выберите ответы для категории "$name" """
        }
      ),
      div(
        cls("content"),
        div(
          cls("description"),
          div(
            cls("ui middle aligned relaxed divided selection list"),
            children <-- values.combineWith(selectedItems).map {
              case (values, selectedValues) =>
                values.map {
                  v =>
                    val selected = selectedValues.contains(v)
                    div(
                      cls("item"),
                      if (selected) i(cls("green check icon")) else emptyNode,
                      div(
                        cls("content"),
                        div(
                          cls("description"),
                          v
                        )
                      ),
                      onClick --> selectedItems.updater[MouseEvent]((vs, _) =>
                        if (selected) vs.filterNot(_ == v) else vs :+ v
                      )
                    )
                }
            }
          )
        )
      ),
      div(
        cls("actions"),
        div(
          cls("ui green approve left labeled icon button"),
          i(cls("checkmark icon")),
          "Добавить"
        ),
        div(
          cls("ui black deny button"),
          "Отмена"
        )
      )
    )
  }
}
