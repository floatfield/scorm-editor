package ru.studyground.domain.user

import zio.{Has, RIO, Task, ZIO}

case class UserExistsException(name: String) extends Exception

trait UserRepoService {
  def create(name: String, password: String): Task[Unit]
  def get(name: String): Task[Option[User]]
  def delete(name: String): Task[Unit]
  def isValid(name: String, password: String): Task[Boolean]
}

object UserRepoService {

  def create(name: String, password: String): RIO[Has[UserRepoService], Unit] =
    ZIO.accessM(_.get[UserRepoService].create(name, password))

  def get(name: String): RIO[Has[UserRepoService], Option[User]] =
    ZIO.accessM(_.get[UserRepoService].get(name))

  def delete(name: String): RIO[Has[UserRepoService], Unit] =
    ZIO.accessM(_.get[UserRepoService].delete(name))

  def isValid(name: String, password: String): RIO[Has[UserRepoService], Boolean] =
    ZIO.accessM(_.get[UserRepoService].isValid(name, password))

}
