package ru
package studyground

import org.rocksdb._
import org.rocksdb.util.SizeUnit
import zio.blocking.{Blocking, effectBlocking}
import zio.console.{Console, putStrLn}
import zio.{RIO, Task, ZIO, ZManaged}

trait Repository[K, V] {
  def put(k: K, v: V): RIO[Blocking, Unit]
  def get(k: K): RIO[Blocking, Option[V]]
  def delete(k: K): RIO[Blocking, Unit]
}

trait RepoCodec[A] {
  def toByteArray(a: A): Task[Array[Byte]]
  def fromByteArray(bytes: Array[Byte]): Task[A]
}

object Repository {

  def mkRepository[K: RepoCodec, V: RepoCodec](
      path: String
  ): ZManaged[Blocking with Console, Throwable, Repository[K, V]] =
    ZManaged
      .make(effectBlocking {
        val options = new Options()
        options
          .setCreateIfMissing(true)
          .setWriteBufferSize(50 * SizeUnit.MB)
          .setMaxWriteBufferNumber(3)
          .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
          .setCompactionStyle(CompactionStyle.UNIVERSAL)
        RocksDB.open(options, path)
      })(store => (putStrLn("closing db") *> effectBlocking(store.close())).orDie)
      .map(s => RocksBdRepo(s))

}

case class RocksBdRepo[K: RepoCodec, V: RepoCodec](rocksDB: RocksDB)
    extends Repository[K, V] {

  private val keyCodec: RepoCodec[K] = implicitly[RepoCodec[K]]
  private val valCodec: RepoCodec[V] = implicitly[RepoCodec[V]]

  override def put(k: K, v: V): RIO[Blocking, Unit] =
    for {
      key <- keyCodec.toByteArray(k)
      value <- valCodec.toByteArray(v)
      _ <- effectBlocking(rocksDB.put(key, value))
    } yield ()

  override def get(k: K): RIO[Blocking, Option[V]] =
    for {
      key <- keyCodec.toByteArray(k)
      mbVal <- effectBlocking(Option(rocksDB.get(key)))
      res <- mbVal.fold[Task[Option[V]]](ZIO.none)(bytes => valCodec.fromByteArray(bytes).map(Some(_)))
    } yield res

  override def delete(k: K): RIO[Blocking, Unit] =
    keyCodec.toByteArray(k).flatMap(key =>
      effectBlocking(rocksDB.delete(key))
    )
}
