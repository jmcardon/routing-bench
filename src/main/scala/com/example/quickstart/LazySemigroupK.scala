package com.example.quickstart

import cats.SemigroupK
import cats.data.Kleisli
import cats.effect.{IO, Sync}
import cats.syntax.all._
import HelloWorldServer._
import org.http4s.{HttpService, Request, Response}

object LazySemigroupK {
  implicit def catsDataSemigroupKForKleisli[F[_], A](
      implicit F0: SemigroupK[F],
      F1: Sync[F]): SemigroupK[Kleisli[F, A, ?]] =
    new SemigroupK[Kleisli[F, A, ?]] {
      override def combineK[B](x: Kleisli[F, A, B],
                               y: Kleisli[F, A, B]): Kleisli[F, A, B] =
        Kleisli(a => F0.combineK(x.run(a), F1.suspend(y.run(a))))
    }

  val composedMany: HttpService[IO] =
    service <+> service2 <+>
      service3 <+> service4 <+>
      service5 <+> service6 <+>
      service7 <+> service8 <+>
      service9 <+> service10

  val CMany: Kleisli[IO, Request[IO], Response[IO]] = composedMany.orNotFound

  val composedBig2: HttpService[IO] = serviceBIG1 <+> serviceBIG2

  val CTwo: Kleisli[IO, Request[IO], Response[IO]] = composedBig2.orNotFound

}
