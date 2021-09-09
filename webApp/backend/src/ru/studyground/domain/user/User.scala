package ru
package studyground
package domain.user

import zio.json._

final case class User(name: String, password: Array[Byte])

object User {
  implicit val encoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  implicit val decoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]
}