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

  def createRequest(numBody: Int, numParts: Int) = {
    val normies: Stream[IO, Byte] =
      Stream
        .emits(
          List
            .fill(numBody)("REEEEEEEEEEEEEEEEEE".getBytes())
            .flatMap(_.toList))
        .covary[IO]

    val ree: Multipart[IO] = Multipart[IO](
      Vector.fill(10)(
        Part[IO](Headers(Header("Content-Type", "text/plain"),
                         Header("Content-Type", "text/plain")),
                 normies)),
      Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")
    )

    val tendies: EntityBody[IO] =
      EntityEncoder
        .multipartEncoder[IO]
        .toEntity(ree)
        .unsafeRunSync
        .body

    Request[IO](body = tendies, headers = ree.headers)
  }

  val requestLarge: Request[IO] =
    createRequest(1000, 100)

  val requestSmall: Request[IO] =
    createRequest(10, 10)

  val requestMedium: Request[IO] =
    createRequest(500, 50)

  val requestHyooge: Request[IO] =
    createRequest(10000, 10)

  val encoderLive: EntityDecoder[IO, Multipart[IO]] =
    EntityDecoder.multipart[IO]

  val encoderNew: EntityDecoder[IO, Multipart[IO]] =
    MultipartParser1.decoder[IO]

}
