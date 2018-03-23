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
  def smallOldMultipart: Multipart[IO] = {
    encoderLive
      .decode(requestSmall, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def smallNewMultipart: Multipart[IO] = {
    encoderNew
      .decode(requestSmall, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def mediumOldMultipart: Multipart[IO] = {
    encoderLive
      .decode(requestMedium, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def mediumNewMultipart: Multipart[IO] = {
    encoderNew
      .decode(requestMedium, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def bigOldMultipart: Multipart[IO] = {
    encoderLive
      .decode(requestLarge, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def bigNewMultipart: Multipart[IO] = {
    encoderNew
      .decode(requestLarge, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def bigPartBodyOldMultipart: Multipart[IO] = {
    encoderLive
      .decode(requestHyooge, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def bigPartBodyNewMultipart: Multipart[IO] = {
    encoderNew
      .decode(requestHyooge, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

}

object MultipartBench {
  val boundary = Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")

  val dumbBodySmall: Stream[IO, Byte] =
    Stream
      .emits(List.fill(10)("REEEEEEEEEEEEEEEEEE".getBytes()).flatMap(_.toList))
      .covary[IO]

  val dumbBodyMedium: Stream[IO, Byte] =
    Stream
      .emits(List.fill(50)("REEEEEEEEEEEEEEEEEE".getBytes()).flatMap(_.toList))
      .covary[IO]

  val dumbBodyLarge: Stream[IO, Byte] =
    Stream
      .emits(
        List.fill(1000)("REEEEEEEEEEEEEEEEEE".getBytes()).flatMap(_.toList))
      .covary[IO]

  val dumbBodyHyooge: Stream[IO, Byte] =
    Stream
      .emits(
        List.fill(100000)("REEEEEEEEEEEEEEEEEE".getBytes()).flatMap(_.toList))
      .covary[IO]

  val multipartSmall: Multipart[IO] = Multipart[IO](
    Vector.fill(10)(
      Part[IO](Headers(Header("Content-Type", "text/plain"),
                       Header("Content-Type", "text/plain")),
               dumbBodySmall)),
    Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")
  )

  val multipartMedium: Multipart[IO] = Multipart[IO](
    Vector.fill(50)(
      Part[IO](Headers(Header("Content-Type", "text/plain"),
                       Header("Content-Type", "text/plain")),
               dumbBodyMedium)),
    Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")
  )

  val multipartLarge: Multipart[IO] = Multipart[IO](
    Vector.fill(100)(
      Part[IO](Headers(Header("Content-Type", "text/plain"),
                       Header("Content-Type", "text/plain")),
               dumbBodyLarge)),
    Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")
  )

  val multipartHyooge: Multipart[IO] = Multipart[IO](
    Vector.fill(10)(
      Part[IO](Headers(Header("Content-Type", "text/plain"),
                       Header("Content-Type", "text/plain")),
               dumbBodyHyooge)),
    Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")
  )

  val byteStreamLarge: EntityBody[IO] =
    EntityEncoder
      .multipartEncoder[IO]
      .toEntity(multipartLarge)
      .unsafeRunSync
      .body

  val byteStreamSmall: EntityBody[IO] =
    EntityEncoder
      .multipartEncoder[IO]
      .toEntity(multipartSmall)
      .unsafeRunSync
      .body

  val byteStreamMedium: EntityBody[IO] =
    EntityEncoder
      .multipartEncoder[IO]
      .toEntity(multipartMedium)
      .unsafeRunSync
      .body

  val byteStreamHyooge: EntityBody[IO] =
    EntityEncoder
      .multipartEncoder[IO]
      .toEntity(multipartHyooge)
      .unsafeRunSync
      .body

  val requestLarge: Request[IO] =
    Request[IO](body = byteStreamLarge, headers = multipartLarge.headers)

  val requestSmall: Request[IO] =
    Request[IO](body = byteStreamSmall, headers = multipartSmall.headers)

  val requestMedium: Request[IO] =
    Request[IO](body = byteStreamMedium, headers = multipartMedium.headers)

  val requestHyooge: Request[IO] =
    Request[IO](body = byteStreamHyooge, headers = multipartHyooge.headers)

  val encoderLive: EntityDecoder[IO, Multipart[IO]] =
    EntityDecoder.multipart[IO]

  val encoderNew: EntityDecoder[IO, Multipart[IO]] =
    MultipartParser1.decoder[IO]

}
