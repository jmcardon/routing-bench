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
  def firstMany: Response[IO] = {
    HelloWorldServer.CMany.run(Bencher.first).unsafeRunSync()
  }

  @Benchmark
  def firstTwo: Response[IO] = {
    HelloWorldServer.CTwo.run(Bencher.first).unsafeRunSync()
  }

  @Benchmark
  def secondTwo: Response[IO] = {
    HelloWorldServer.CTwo.run(Bencher.second).unsafeRunSync()
  }

  @Benchmark
  def secondMany: Response[IO] = {
    HelloWorldServer.CMany.run(Bencher.second).unsafeRunSync()
  }

  @Benchmark
  def thirdTwo: Response[IO] = {
    HelloWorldServer.CTwo.run(Bencher.third).unsafeRunSync()
  }

  @Benchmark
  def thirdMany: Response[IO] = {
    HelloWorldServer.CMany.run(Bencher.third).unsafeRunSync()
  }

  @Benchmark
  def tenthTwo: Response[IO] = {
    HelloWorldServer.CTwo.run(Bencher.tenth).unsafeRunSync()
  }

  @Benchmark
  def tenthMany: Response[IO] = {
    HelloWorldServer.CMany.run(Bencher.tenth).unsafeRunSync()
  }

  @Benchmark
  def eleventhTwo: Response[IO] = {
    HelloWorldServer.CTwo.run(Bencher.eleventh).unsafeRunSync()
  }

  @Benchmark
  def eleventhMany: Response[IO] = {
    HelloWorldServer.CMany.run(Bencher.eleventh).unsafeRunSync()
  }

  @Benchmark
  def lastMany: Response[IO] = {
    HelloWorldServer.CMany.run(Bencher.last).unsafeRunSync()
  }

  @Benchmark
  def lastTwo: Response[IO] = {
    HelloWorldServer.CTwo.run(Bencher.last).unsafeRunSync()
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
