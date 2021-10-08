package ru.studyground

sealed trait Command

object Command {
  final case class SetToken(token: String) extends Command
  case object Logout extends Command

  def dispatch(state: MainAppState, command: Command): MainAppState =
    command match {
      case Command.SetToken(token) =>
        state.setToken(token)
      case Command.Logout =>
        state.logout
      case c: LoginCommand =>
        state.setLoginState(LoginCommand.dispatch(state.loginState, c))
      case c: BucketsTaskCommand =>
        state.setBucketsTaskState(BucketsTaskCommand.dispatch(state.bucketsTaskState, c))
    }
}

sealed trait LoginCommand extends Command

object LoginCommand {
  final case class SetName(value: String) extends LoginCommand
  final case class SetPassword(value: String) extends LoginCommand
  final case class SetFormState(fs: FormState) extends LoginCommand

  def dispatch(state: LoginState, command: LoginCommand): LoginState =
    command match {
      case LoginCommand.SetName(newName) =>
        state.copy(name = newName)
      case LoginCommand.SetPassword(newPassword) =>
        state.copy(password = newPassword)
      case LoginCommand.SetFormState(formState) =>
        state.copy(formState = formState)
    }
}

sealed trait BucketsTaskCommand extends Command

object BucketsTaskCommand {
  final case class SetName(value: String) extends BucketsTaskCommand
  final case class SetDescription(value: String) extends BucketsTaskCommand
  final case class SetTask(value: String) extends BucketsTaskCommand
  final case class SetFormState(formState: FormState) extends BucketsTaskCommand
  final case class SetState(bucketsTaskState: BucketsTaskState) extends BucketsTaskCommand

  def dispatch(state: BucketsTaskState, command: BucketsTaskCommand): BucketsTaskState =
    command match {
      case SetName(name) =>
        state.copy(name = name)
      case SetDescription(descr) =>
        state.copy(description = descr)
      case SetTask(task) =>
        state.copy(task = task)
      case SetFormState(formState) => formState match {
        case FormState.Success =>
          BucketsTaskState.empty.copy(formState = formState)
        case _ =>
          state.copy(formState = formState)
      }
      case SetState(s) => s
    }
}
