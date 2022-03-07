package ru.studyground.solver

import com.raquo.laminar.api.L._
import org.scalajs.dom.{DragEvent, MouseEvent}
import ru.studyground.{BucketDTO => Bucket}

object AnswerBucket {
  def renderBucket(
      id: Int,
      bucketNames: Signal[List[String]],
      bucket: Signal[Bucket],
      commandObserver: Observer[Command]
  ): Div = {
    val eventToCommand: DragEvent => Option[Command] = (e: DragEvent) =>
      Option(e.dataTransfer.getData("text")).collect {
        case answerId if answerId.nonEmpty =>
          Command.AddAnswer(id, answerId)
      }
    val answerId = s"answer_bucket_$id"
    div(
      idAttr(answerId),
      cls("ui segments"),
      div(
        cls("ui blue segment"),
        Dropdown(
          defaultText = "Select group name",
          options = bucketNames,
          onChange = (name: Option[String]) =>
            commandObserver.onNext(
              name.fold[Command](Command.ClearBucketName(id))(n =>
                Command.SelectBucketName(id, n)
              )
            ),
          text = bucket.map(b => Option(b.name).filter(_.nonEmpty)),
          parentId = answerId
        )
      ),
      div(
        cls("ui segment"),
        children <-- bucket.map(
          _.values.map(value =>
            div(
              cls("ui clearing segment"),
              div(
                cls("ui top right attached label"),
                i(cls("close icon")),
                onClick --> commandObserver.contramap[MouseEvent](_ =>
                  Command.RemoveAnswer(id, value)
                )
              ),
              value
            )
          )
        ),
        div(
          cls("ui blue labeled basic icon button"),
          i(cls("plus icon")),
          onClick --> commandObserver.contramap[MouseEvent](_ =>
            Command.ShowModal(id)
          ),
          "Добавить ответ"
        )
      ),
      onDragOver --> Observer[DragEvent](_.preventDefault()),
      onDrop --> commandObserver.contracollect[DragEvent](eventToCommand.unlift)
    )
  }
}
