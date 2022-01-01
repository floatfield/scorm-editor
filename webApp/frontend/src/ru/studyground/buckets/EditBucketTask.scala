package ru.studyground.buckets

import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import org.scalajs.dom.console
import ru.studyground._
import ru.studyground.common.{Form, TextFileInput}
import zio.json._

object EditBucketTask {
  def render(
      router: Router[Page],
      $state: Signal[MainAppState],
      $id: Signal[Option[BucketsTaskId]],
      commandObserver: Observer[BucketsTaskCommand]
  ): Div = {
    val $taskState = $state.map(_.bucketsTaskState)
    val newBucketTaskId = "new-bucket-task"
    val formContent = div(
      div(
        cls("field"),
        label("Название"),
        input(
          typ("text"),
          name("name"),
          value <-- $taskState.map(_.name),
          onChange.preventDefault.mapToValue --> commandObserver
            .contramap[String](BucketsTaskCommand.SetName)
        )
      ),
      div(
        cls("field"),
        label("Описание"),
        textArea(
          value <-- $taskState.map(_.description),
          onChange.preventDefault.mapToValue --> commandObserver
            .contramap[String](BucketsTaskCommand.SetDescription)
        )
      ),
      div(
        cls("field"),
        TextFileInput(
          newBucketTaskId,
          commandObserver.contramap[String](BucketsTaskCommand.SetTask)
        ),
        label(
          forId(newBucketTaskId),
          cls("ui basic blue button"),
          "Выберите файл с заданием"
        )
      ),
      div(
        cls("field"),
        div(
          cls("ui primary button"),
          "Отправить",
          inContext { node =>
            val $click = node.events(onClick.preventDefault).sample($state)
            val $req = $click.flatMap {
              s =>
                val headers = Map(
                  "Content-Type" -> "application/json"
                )
                val url = "/buckets"
                val $ajax = s.bucketsTaskState.id match {
                  case Some(id) =>
                    AjaxEventStream
                      .put(
                        s"/buckets/${id.value.toString}",
                        s.bucketsTaskState.bucketsTaskDTO.toJson,
                        headers = headers
                      )
                  case None =>
                    AjaxEventStream
                      .post(
                        url,
                        s.bucketsTaskState.bucketsTaskDTO.toJson,
                        headers = headers
                      )
                }
                $ajax
                  .map(r => Right(r.responseText))
                  .recover {
                    case err: AjaxStreamError =>
                      console.error(err.getMessage)
                      Some(Left(err.xhr.responseText))
                  }
            }
            List(
              $click --> commandObserver.contramap[MainAppState](_ =>
                BucketsTaskCommand.SetFormState(FormState.Loading)
              ),
              $req --> commandObserver.contramap[Either[String, String]](
                _.fold[BucketsTaskCommand](
                  err =>
                    BucketsTaskCommand.SetFormState(FormState.Error(err)),
                  _ => BucketsTaskCommand.SetFormState(FormState.Editing)
                )
              ),
              $req --> commandObserver
                .contracollect[Either[String, String]] {
                  case Right(_) =>
                    BucketsTaskCommand.SetFormState(FormState.Success)
                }
            )
          }
        )
      )
    )
    Form(
      formContent,
      $state.map(_.bucketsTaskState.formState),
      commandObserver.contramap[Unit](_ => BucketsTaskCommand.SetFormState(FormState.Editing))
    ).amend(
      $state.map(_.bucketsTaskState.formState) --> Observer[FormState] {
        case FormState.Success =>
          commandObserver.onNext(
            BucketsTaskCommand.SetFormState(FormState.Editing)
          )
          router.pushState(BucketsPage.ListBucketTasks)
        case _ =>
      },
      $id.withCurrentValueOf($state.map(_.bucketsTaskList)) --> commandObserver
        .contramap[(Option[BucketsTaskId], List[BucketsTask])] {
          case (Some(id), ts) if ts.exists(_.id == id) =>
            BucketsTaskCommand.SetState(
              BucketsTaskState.fromBucketsTask(ts.find(_.id == id).get)
            )
          case (None, _) =>
            BucketsTaskCommand.SetState(BucketsTaskState.empty)
        }
    )
  }
}
