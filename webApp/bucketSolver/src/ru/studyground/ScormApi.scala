package ru.studyground

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal
object ScormApi extends js.Object {
  def lmsSetValue(name: String, value: String): Unit = js.native
  def lmsCommit(): Unit = js.native
  def lmsFinish(): Unit = js.native
}