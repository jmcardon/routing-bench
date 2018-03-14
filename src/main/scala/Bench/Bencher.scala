package Bench
import java.util.concurrent.TimeUnit

import cats.effect.IO
import org.openjdk.jmh.annotations._
import com.example.quickstart._
import org.http4s._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class Bencher {

  @Benchmark
  def firstMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.first)
  }

  @Benchmark
  def firstTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.first)
  }

  @Benchmark
  def secondTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.second)
  }

  @Benchmark
  def secondMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.second)
  }

  @Benchmark
  def thirdTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.third)
  }

  @Benchmark
  def thirdMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.third)
  }

  @Benchmark
  def tenthTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.tenth)
  }

  @Benchmark
  def tenthMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.tenth)
  }

  @Benchmark
  def eleventhTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.eleventh)
  }

  @Benchmark
  def eleventhMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.eleventh)
  }

  @Benchmark
  def lastMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.last)
  }

  @Benchmark
  def lastTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.last)
  }

}

object Bencher {
  val first: Request[IO] =
    Request[IO](uri = Uri(path = s"/route1/route11/hi"))
  val second: Request[IO] =
    Request[IO](uri = Uri(path = s"/route2/route22/route222/hi"))
  val third: Request[IO] =
    Request[IO](uri = Uri(path = s"/route3/route33/hi"))
  val tenth: Request[IO] =
    Request[IO](uri = Uri(path = s"/route10/route1010/route101010/hi"))
  val eleventh: Request[IO] =
    Request[IO](uri = Uri(path = s"/route11/route1111/hi"))
  val last: Request[IO] =
    Request[IO](uri = Uri(path = s"/route20/route2020/route202020/hi"))
}
