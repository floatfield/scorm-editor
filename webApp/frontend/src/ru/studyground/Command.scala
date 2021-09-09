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
    }
}

sealed trait LoginCommand extends Command

object LoginCommand {
  final case class SetName(value: String) extends LoginCommand
  final case class SetPassword(value: String) extends LoginCommand

  def dispatch(state: LoginState, command: LoginCommand): LoginState =
    command match {
      case LoginCommand.SetName(newName) =>
        state.copy(name = newName)
      case LoginCommand.SetPassword(newPassword) =>
        state.copy(password = newPassword)
    }
}
