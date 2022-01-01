package ru.studyground

final case class MainAppState(
    loginState: LoginState,
    bucketsTaskState: BucketsTaskState,
    bucketsTaskList: List[BucketsTask],
    isLoggedIn: Boolean
) { self =>
  def setLoginState(l: LoginState): MainAppState =
    self.copy(loginState = l)
  def setBucketsTaskState(s: BucketsTaskState): MainAppState =
    self.copy(bucketsTaskState = s)
  def setBucketsTasks(tasks: List[BucketsTask]): MainAppState =
    self.copy(bucketsTaskList = tasks)
  def prependBucketsTasks(tasks: List[BucketsTask]): MainAppState =
    self.copy(bucketsTaskList = (tasks ++ bucketsTaskList).distinct)
  def updateBucketsTask(task: BucketsTask): MainAppState =
    self.copy(bucketsTaskList = bucketsTaskList.map {
      case t if t.id == task.id => task
      case t => t
    })
  def removeBucketsTasks(ids: List[BucketsTaskId]): MainAppState =
    self.copy(bucketsTaskList =
      bucketsTaskList.filterNot(t => ids.contains(t.id))
    )
}

object MainAppState {
  val empty: MainAppState =
    MainAppState(
      loginState = LoginState.empty,
      bucketsTaskState = BucketsTaskState.empty,
      bucketsTaskList = Nil,
      isLoggedIn = false
    )
}

sealed trait FormState

object FormState {
  case object Editing extends FormState
  case object Loading extends FormState
  final case class Error(msg: String) extends FormState
  case object Success extends FormState
}

final case class LoginState(
    name: String,
    password: String,
    formState: FormState
) {
  val loginDTO: LoginDTO = LoginDTO(login = name, password = password)
}

object LoginState {
  val empty: LoginState =
    LoginState(name = "", password = "", formState = FormState.Editing)
}

final case class BucketsTaskState(
    id: Option[BucketsTaskId],
    name: String,
    description: String,
    task: String,
    formState: FormState
) {
  val bucketsTaskDTO: BucketsTaskDTO = BucketsTaskDTO(
    name = name,
    description = description,
    task = task
  )
}

object BucketsTaskState {
  val empty: BucketsTaskState = BucketsTaskState(
    id = None,
    name = "",
    description = "",
    task = "",
    formState = FormState.Editing
  )

  def fromBucketsTask(t: BucketsTask): BucketsTaskState =
    BucketsTaskState(
      id = Some(t.id),
      name = t.name,
      description = t.description,
      task = t.bucketsTaskDTO.task,
      formState = FormState.Editing
    )
}
