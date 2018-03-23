package Bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import cats.effect.IO
import com.example.quickstart.MultipartParser1
import fs2.Stream
import org.http4s._
import org.http4s.headers._
import org.http4s.multipart._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class MultipartBench {
  import MultipartBench._

  @Benchmark
  def smalloldMultipart: Multipart[IO] = {
    encoderLive
      .decode(requestSmall, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def smallnewMultipart: Multipart[IO] = {
    encoderNew
      .decode(requestSmall, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def bigoldMultipart: Multipart[IO] = {
    encoderLive
      .decode(requestLarge, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def bignewMultipart: Multipart[IO] = {
    encoderNew
      .decode(requestLarge, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

}

object MultipartBench {
  val boundary = Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")

  val dumbBodyLarge: Stream[IO, Byte] =
    Stream
      .emits(
        List.fill(1000)("REEEEEEEEEEEEEEEEEE".getBytes()).flatMap(_.toList))
      .covary[IO]

  val bodyLarge: Multipart[IO] = Multipart[IO](
    Vector.fill(100)(
      Part[IO](Headers(Header("Content-Type", "text/plain"),
                       Header("Content-Type", "text/plain")),
               dumbBodyLarge)),
    Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")
  )

  val dumbBodySmall: Stream[IO, Byte] =
    Stream
      .emits(List.fill(10)("REEEEEEEEEEEEEEEEEE".getBytes()).flatMap(_.toList))
      .covary[IO]

  val bodySmall: Multipart[IO] = Multipart[IO](
    Vector.fill(10)(
      Part[IO](Headers(Header("Content-Type", "text/plain"),
                       Header("Content-Type", "text/plain")),
               dumbBodySmall)),
    Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")
  )

  val byteStreamLarge: EntityBody[IO] =
    EntityEncoder.multipartEncoder[IO].toEntity(bodyLarge).unsafeRunSync.body

  val byteStreamSmall: EntityBody[IO] =
    EntityEncoder.multipartEncoder[IO].toEntity(bodySmall).unsafeRunSync.body

  val requestLarge: Request[IO] =
    Request[IO](body = byteStreamLarge, headers = bodyLarge.headers)

  val requestSmall: Request[IO] =
    Request[IO](body = byteStreamSmall, headers = bodySmall.headers)

  val encoderLive: EntityDecoder[IO, Multipart[IO]] =
    EntityDecoder.multipart[IO]

  val encoderNew: EntityDecoder[IO, Multipart[IO]] =
    MultipartParser1.decoder[IO]

}
