package ru.studyground

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class LoginDTO(
    login: String,
    password: String
)

object LoginDTO {
  implicit val loginDTOCodec: JsonCodec[LoginDTO] =
    DeriveJsonCodec.gen[LoginDTO]
}
