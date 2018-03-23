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
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class MultipartBench {
  import MultipartBench._

  @Benchmark
  def oldMultipartStrict: Multipart[IO] = {
    encoderLive
      .decode(request, true)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def newMultipartStrict: Multipart[IO] = {
    encoderNew
      .decode(request, true)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def oldMultipartNotStrict: Multipart[IO] = {
    encoderLive
      .decode(request, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

  @Benchmark
  def newMultipartNotStrict: Multipart[IO] = {
    encoderNew
      .decode(request, false)
      .value
      .unsafeRunSync() match {
      case Right(r) => r
      case Left(l)  => throw l
    }
  }

}

object MultipartBench {
  val dumbBody: Stream[IO, Byte] =
    Stream
      .emits(
        List.fill(1000)("REEEEEEEEEEEEEEEEEE".getBytes()).flatMap(_.toList))
      .covary[IO]

  val boundary =  Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")

  val bodyLarge: Multipart[IO] = Multipart[IO](
    Vector.fill(100)(
      Part[IO](Headers(Header("Content-Type", "text/plain"),
                       Header("Content-Type", "text/plain")),
               dumbBody)),
    Boundary("----WebKitFormBoundarycaZFo8IAKVROTEeD")
  )

  val byteStream: EntityBody[IO] =
    EntityEncoder.multipartEncoder[IO].toEntity(bodyLarge).unsafeRunSync.body

  val request = Request[IO](body = byteStream, headers = bodyLarge.headers)

  val encoderLive: EntityDecoder[IO, Multipart[IO]] =
    EntityDecoder.multipart[IO]

  val encoderNew: EntityDecoder[IO, Multipart[IO]] =
    MultipartParser1.decoder[IO]

}
