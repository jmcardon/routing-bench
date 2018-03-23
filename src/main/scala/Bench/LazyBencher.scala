package Bench
import java.util.concurrent.TimeUnit

import cats.effect.IO
import org.openjdk.jmh.annotations._
import com.example.quickstart.LazySemigroupK
import org.http4s._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class LazyBencher {

  @Benchmark
  def firstMany: Response[IO] = {
    LazySemigroupK.CMany.run(Bencher.first).unsafeRunSync()
  }

  @Benchmark
  def firstTwo: Response[IO] = {
    LazySemigroupK.CTwo.run(Bencher.first).unsafeRunSync()
  }

  @Benchmark
  def secondTwo: Response[IO] = {
    LazySemigroupK.CTwo.run(Bencher.second).unsafeRunSync()
  }

  @Benchmark
  def secondMany: Response[IO] = {
    LazySemigroupK.CMany.run(Bencher.second).unsafeRunSync()
  }

  @Benchmark
  def thirdTwo: Response[IO] = {
    LazySemigroupK.CTwo.run(Bencher.third).unsafeRunSync()
  }

  @Benchmark
  def thirdMany: Response[IO] = {
    LazySemigroupK.CMany.run(Bencher.third).unsafeRunSync()
  }

  @Benchmark
  def tenthTwo: Response[IO] = {
    LazySemigroupK.CTwo.run(Bencher.tenth).unsafeRunSync()
  }

  @Benchmark
  def tenthMany: Response[IO] = {
    LazySemigroupK.CMany.run(Bencher.tenth).unsafeRunSync()
  }

  @Benchmark
  def eleventhTwo: Response[IO] = {
    LazySemigroupK.CTwo.run(Bencher.eleventh).unsafeRunSync()
  }

  @Benchmark
  def eleventhMany: Response[IO] = {
    LazySemigroupK.CMany.run(Bencher.eleventh).unsafeRunSync()
  }

  @Benchmark
  def lastMany: Response[IO] = {
    LazySemigroupK.CMany.run(Bencher.last).unsafeRunSync()
  }

  @Benchmark
  def lastTwo: Response[IO] = {
    LazySemigroupK.CTwo.run(Bencher.last).unsafeRunSync()
  }

}
