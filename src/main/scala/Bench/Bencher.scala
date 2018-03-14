package Bench
import java.util.concurrent.TimeUnit
import javax.crypto
import javax.crypto.Cipher

import cats.effect.IO
import org.openjdk.jmh.annotations._
import com.example.quickstart._
import org.http4s._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class Bencher {

  @Benchmark
  def firstAndLastMany: Array[IO[Response[IO]]] = {
    Array(HelloWorldServer.CMany.run(Bencher.routes(0)),
          HelloWorldServer.CMany.run(Bencher.routes(19)))
  }

  @Benchmark
  def firstAndLastTwo: Array[IO[Response[IO]]] = {
    Array(HelloWorldServer.CTwo.run(Bencher.routes(0)),
          HelloWorldServer.CTwo.run(Bencher.routes(19)))
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
  def lastMany: IO[Response[IO]] = {
    HelloWorldServer.CMany.run(Bencher.routes(19))
  }

  @Benchmark
  def lastTwo: IO[Response[IO]] = {
    HelloWorldServer.CTwo.run(Bencher.routes(19))
  }

  @Benchmark
  def randomOrderTwo: Array[IO[Response[IO]]] = {
    Array(
      HelloWorldServer.CTwo.run(Bencher.routes(19)),
      HelloWorldServer.CTwo.run(Bencher.routes(5)),
      HelloWorldServer.CTwo.run(Bencher.routes(6)),
      HelloWorldServer.CTwo.run(Bencher.routes(10)),
      HelloWorldServer.CTwo.run(Bencher.routes(1)),
      HelloWorldServer.CTwo.run(Bencher.routes(2)),
      HelloWorldServer.CTwo.run(Bencher.routes(12)),
      HelloWorldServer.CTwo.run(Bencher.routes(18)),
      HelloWorldServer.CTwo.run(Bencher.routes(13))
    )
  }

  @Benchmark
  def randomOrderMany: Array[IO[Response[IO]]] = {
    Array(
      HelloWorldServer.CMany.run(Bencher.routes(19)),
      HelloWorldServer.CMany.run(Bencher.routes(5)),
      HelloWorldServer.CMany.run(Bencher.routes(6)),
      HelloWorldServer.CMany.run(Bencher.routes(10)),
      HelloWorldServer.CMany.run(Bencher.routes(1)),
      HelloWorldServer.CMany.run(Bencher.routes(2)),
      HelloWorldServer.CMany.run(Bencher.routes(12)),
      HelloWorldServer.CMany.run(Bencher.routes(18)),
      HelloWorldServer.CMany.run(Bencher.routes(13))
    )
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
