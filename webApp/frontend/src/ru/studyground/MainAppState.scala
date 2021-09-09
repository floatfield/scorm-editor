package ru.studyground

final case class MainAppState(
    token: Option[String],
    loginState: LoginState
) { self =>
  def setToken(t: String): MainAppState =
    self.copy(token = Some(t))
  def logout: MainAppState =
    self.copy(
      token = None,
      loginState = LoginState.empty
    )
  def setLoginState(l: LoginState): MainAppState =
    self.copy(loginState = l)
}

object MainAppState {
  val empty: MainAppState =
    MainAppState(
      token = None,
      loginState = LoginState.empty
    )
}

final case class LoginState(
    name: String,
    password: String
) {
  val loginDTO: LoginDTO = LoginDTO(login = name, password = password)
}

object LoginState {
  val empty: LoginState =
    LoginState(name = "", password = "")
}

final case class NewBucketsTaskState(
    name: String,
    description: String,
    task: String
)
