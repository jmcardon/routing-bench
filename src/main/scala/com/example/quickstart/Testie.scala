package com.example.quickstart

import cats.effect._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze.Http1Client
import org.http4s.multipart.Multipart

case class Test(s: String)

object Testie {

  def main(args: Array[String]): Unit = {
    println(
      Http1Client
        .stream[IO]()
        .evalMap { client =>
          client.expect[String](
            Request[IO](
              Method.POST,
              Uri.uri("http://localhost:9999/")))
//              body = Stream.emits(Test("123").asJson.toString().getBytes())))
        }
        .compile
        .last
        .unsafeRunSync
        .get)
  }

}
