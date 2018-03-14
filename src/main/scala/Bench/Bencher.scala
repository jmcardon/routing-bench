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
  def lastMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.routes(19))
  }

  @Benchmark
  def lastTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.routes(19))
  }

  @Benchmark
  def firstMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.routes(0))
  }

  @Benchmark
  def firstTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.routes(0))
  }

  @Benchmark
  def middleTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.routes(10))
  }

  @Benchmark
  def middleMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.routes(10))
  }

}

object Bencher {
  val routes: Array[Request[IO]] =
    Array.range(1, 21).map { i =>
      if (i % 2 == 0)
        Request[IO](uri = Uri(path = s"/route$i/route$i$i/route$i$i$i/LOL"))
      else
        Request[IO](uri = Uri(path = s"/route$i/route$i$i/hi"))
    }
}
