package ru.studyground.domain.buckets.task

import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema}
import ru.studyground.BucketsTask
import ru.studyground.{RepoCodec, StreamDecoder}
import zio.stream.ZStream
import zio.{Task, ZIO}

import java.io.{ByteArrayOutputStream, InputStream}
import java.util.UUID
import scala.util.Success

final case class DeserializationException(bytes: Array[Byte], className: String)
    extends Exception

object RepoCodecs {

  implicit val uuidCodec: RepoCodec[UUID] = new RepoCodec[UUID] {

    override def toByteArray(uuid: UUID): Task[Array[Byte]] =
      ZIO.succeed(
        BigInt(uuid.getMostSignificantBits).toByteArray ++
          BigInt(uuid.getLeastSignificantBits).toByteArray
      )

    override def fromByteArray(bytes: Array[Byte]): Task[UUID] =
      ZIO.effect(
        new UUID(
          BigInt(bytes.slice(0, 8)).toLong,
          BigInt(bytes.slice(8, 16)).toLong
        )
      )

  }

  implicit val bucketsTaskCodec: RepoCodec[BucketsTask] =
    new RepoCodec[BucketsTask] {

      private val schema = AvroSchema[BucketsTask]

      override def toByteArray(a: BucketsTask): Task[Array[Byte]] =
        Task.effect {
          val o = new ByteArrayOutputStream()
          val os = AvroOutputStream.binary[BucketsTask].to(o).build()
          os.write(a)
          os.flush()
          os.close()
          o.toByteArray
        }

      override def fromByteArray(bytes: Array[Byte]): Task[BucketsTask] =
        for {
          parsed <- Task.effect {
            val is =
              AvroInputStream.binary[BucketsTask].from(bytes).build(schema)
            is.tryIterator.toList.headOption
          }
          res <- parsed match {
            case Some(Success(value)) =>
              ZIO.succeed(value)
            case _ =>
              ZIO.fail(
                DeserializationException(bytes, BucketsTask.getClass.getName)
              )
          }
        } yield res
    }

  implicit val bucketsTaskStreamDecoder: StreamDecoder[BucketsTask] = new StreamDecoder[BucketsTask] {
    private val schema = AvroSchema[BucketsTask]

    override def fromInputStream(is: InputStream): ZStream[Any, Throwable, BucketsTask] =
      for {
        it <- ZStream.fromEffect(ZIO.effect{
          val avroIs = AvroInputStream.binary[BucketsTask].from(is).build(schema)
          avroIs.iterator
        })
        b <- ZStream.fromIterator(it)
      } yield b
  }
}
