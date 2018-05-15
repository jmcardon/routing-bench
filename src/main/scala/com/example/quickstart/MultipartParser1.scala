package com.example.quickstart

import java.nio.charset.StandardCharsets

import cats.effect._
import cats.implicits.{catsSyntaxEither => _, _}
import fs2._
import org.http4s._
import org.http4s.multipart._
import org.http4s.util._

/** A low-level multipart-parsing pipe.  Most end users will prefer EntityDecoder[Multipart]. */
object MultipartParser1 {
  def decoder[F[_]: Sync]: EntityDecoder[F, Multipart[F]] =
    EntityDecoder.decodeBy(MediaRange.`multipart/*`) { msg =>
      msg.contentType.flatMap(_.mediaType.extensions.get("boundary")) match {
        case Some(boundary) =>
          DecodeResult {
            msg.body
              .through(parseToPartsStream[F](Boundary(boundary)))
              .compile
              .toVector
              .map[Either[DecodeFailure, Multipart[F]]](parts =>
                Right(Multipart(parts, Boundary(boundary))))
              .handleError {
                case e: InvalidMessageBodyFailure => Left(e)
                case e =>
                  Left(
                    InvalidMessageBodyFailure("Invalid multipart body",
                                              Some(e)))
              }
          }
        case None =>
          DecodeResult.failure(
            InvalidMessageBodyFailure(
              "Missing boundary extension to Content-Type"))
      }
    }

  /** Converts ASCII encoded byte stream to a stream of `String`. */
  def asciiDecode[F[_]]: Pipe[F, Byte, String] =
    _.chunks.through(asciiDecodeC)

  private def asciiCheck(b: Byte) = 0x80 & b

  /** Converts ASCII encoded `Chunk[Byte]` inputs to `String`. */
  def asciiDecodeC[F[_]]: Pipe[F, Chunk[Byte], String] = { in =>
    def tailRecAsciiCheck(i: Int, bytes: Array[Byte]): Stream[F, String] =
      if (i == bytes.length)
        Stream.emit(new String(bytes, StandardCharsets.US_ASCII))
      else {
        if (asciiCheck(bytes(i)) == 0x80) {
          Stream.raiseError(
            new IllegalArgumentException(
              "byte stream is not encodable as ascii bytes"))
        } else {
          tailRecAsciiCheck(i + 1, bytes)
        }
      }

    in.flatMap(c => tailRecAsciiCheck(0, c.toArray))
  }


  private[this] val CRLFBytesN = Array[Byte]('\r', '\n')
  private[this] val DoubleCRLFBytesN = Array[Byte]('\r', '\n', '\r', '\n')
  private[this] val DashDashBytesN = Array[Byte]('-', '-')
  private[this] val BoundaryBytesN: Boundary => Array[Byte] = boundary =>
    boundary.value.getBytes("UTF-8")
  val StartLineBytesN: Boundary => Array[Byte] = BoundaryBytesN.andThen(DashDashBytesN ++ _)

  private[this] val ExpectedBytesN: Boundary => Array[Byte] =
    BoundaryBytesN.andThen(CRLFBytesN ++ DashDashBytesN ++ _)
  private[this] val dashByte: Byte = '-'.toByte
  private[this] val streamEmpty = Stream.empty
  private[this] val PullUnit = Pull.pure[Pure, Unit](())

  private type SplitStream[F[_]] = Pull[F, Nothing, (Stream[F, Byte], Stream[F, Byte])]

  def parseStreamed[F[_]: Sync](
    boundary: Boundary,
    limit: Int = 1024): Pipe[F, Byte, Multipart[F]] = { st =>
    ignorePrelude[F](boundary, st, limit)
      .fold(Vector.empty[Part[F]])(_ :+ _)
      .map(Multipart(_, boundary))
  }

  def parseToPartsStream[F[_]: Sync](
    boundary: Boundary,
    limit: Int = 1024): Pipe[F, Byte, Part[F]] = { st =>
    ignorePrelude[F](boundary, st, limit)
  }

  private def splitAndIgnorePrev[F[_]](
    values: Array[Byte],
    state: Int,
    c: Chunk[Byte]): (Int, Stream[F, Byte]) = {
    var i = 0
    var currState = state
    val len = values.length
    while (currState < len && i < c.size) {
      if (c(i) == values(currState)) {
        currState += 1
      } else if (c(i) == values(0)) {
        currState = 1
      } else {
        currState = 0
      }
      i += 1
    }

    if (currState == 0) {
      (0, Stream.empty)
    } else if (currState == len) {
      (currState, Stream.chunk(c.drop(i)))
    } else {
      (currState, Stream.empty)
    }
  }

  /** Split a chunk in the case of a complete match:
    *
    * If it is a chunk that is between a partial match
    * (middleChunked), consider the prior partial match
    * as part of the data to emit.
    *
    * If it is a fully matched, fresh chunk (no carry over partial match),
    * emit everything until the match, and everything after the match.
    *
    * If it is the continuation of a partial match,
    * emit everything after the partial match.
    *
    */
  private def splitCompleteMatch[F[_]: Sync](
    middleChunked: Boolean,
    sti: Int,
    i: Int,
    acc: Stream[F, Byte],
    carry: Stream[F, Byte],
    c: Chunk[Byte]
  ): (Int, Stream[F, Byte], Stream[F, Byte]) =
    if (middleChunked) {
      (
        sti,
        //Emit the partial match as well
        acc ++ carry ++ Stream.chunk(c.take(i - sti)),
        Stream.chunk(c.drop(i))) //Emit after the match
    } else {
      (
        sti,
        acc, //block completes partial match, so do not emit carry
        Stream.chunk(c.drop(i))) //Emit everything after the match
    }

  /** Split a chunk in the case of a partial match:
    *
    * DO NOT USE. Was made private because
    * Jose messed up hard like 5 patches ago and now it breaks bincompat to
    * remove.
    *
    */
  private def splitPartialMatch[F[_]: Sync](
    state: Int,
    middleChunked: Boolean,
    currState: Int,
    i: Int,
    acc: Stream[F, Byte],
    carry: Stream[F, Byte],
    c: Chunk[Byte]
  ): (Int, Stream[F, Byte], Stream[F, Byte]) = {
    val ixx = i - currState
    if (middleChunked || state == 0) {
      val (lchunk, rchunk) = c.splitAt(ixx)
      (currState, acc ++ carry ++ Stream.chunk(lchunk), Stream.chunk(rchunk))
    } else {
      (currState, acc, carry ++ Stream.chunk(c))
    }
  }

  /** Split a chunk in the case of a partial match:
    *
    * If it is a chunk that is between a partial match
    * (middle chunked), the prior partial match is added to
    * the accumulator, and the current partial match is
    * considered to carry over.
    *
    * If it is a fresh chunk (no carry over partial match),
    * everything prior to the partial match is added to the accumulator,
    * and the partial match is considered the carry over.
    *
    * Else, if the whole block is a partial match,
    * add it to the carry over
    *
    */
  private def splitPartialMatch0[F[_]: Sync](
    middleChunked: Boolean,
    currState: Int,
    i: Int,
    acc: Stream[F, Byte],
    carry: Stream[F, Byte],
    c: Chunk[Byte]
  ): (Int, Stream[F, Byte], Stream[F, Byte]) = {
    val ixx = i - currState
    if (middleChunked) {
      val (lchunk, rchunk) = c.splitAt(ixx)
      (currState, acc ++ carry ++ Stream.chunk(lchunk), Stream.chunk(rchunk))
    } else {
      (currState, acc, carry ++ Stream.chunk(c))
    }
  }

  /** Split a chunk as part of either a left or right
    * stream depending on the byte sequence in `values`.
    *
    * `state` represents the current counter position
    * for `values`, which is necessary to keep track of in the
    * case of partial matches.
    *
    * `acc` holds the cumulative left stream values,
    * and `carry` holds the values that may possibly
    * be the byte sequence. As such, carry is re-emitted if it was an
    * incomplete match, or ignored (as such excluding the sequence
    * from the subsequent split stream).
    *
    */
  private def splitOnChunk[F[_]: Sync](
    values: Array[Byte],
    state: Int,
    c: Chunk[Byte],
    acc: Stream[F, Byte],
    carry: Stream[F, Byte]): (Int, Stream[F, Byte], Stream[F, Byte]) = {
    var i = 0
    var currState = state
    val len = values.length
    while (currState < len && i < c.size) {
      if (c(i) == values(currState)) {
        currState += 1
      } else if (c(i) == values(0)) {
        currState = 1
      } else {
        currState = 0
      }
      i += 1
    }
    //It will only be zero if
    //the chunk matches from the very beginning,
    //since currstate can never be greater than
    //(i + state).
    val middleChunked = i + state - currState > 0

    if (currState == 0) {
      (0, acc ++ carry ++ Stream.chunk(c), Stream.empty)
    } else if (currState == len) {
      splitCompleteMatch(middleChunked, currState, i, acc, carry, c)
    } else {
      splitPartialMatch0(middleChunked, currState, i, acc, carry, c)
    }
  }

  /** The first part of our streaming stages:
    *
    * Ignore the prelude and remove the first boundary. Only traverses until the first
    * part
    */
  private[this] def ignorePrelude[F[_]: Sync](
    b: Boundary,
    stream: Stream[F, Byte],
    limit: Int): Stream[F, Part[F]] = {
    val values = StartLineBytesN(b)

    def go(s: Stream[F, Byte], state: Int, strim: Stream[F, Byte]): Pull[F, Part[F], Unit] =
      if (state == values.length) {
        pullParts[F](b, strim ++ s, limit)
      } else {
        s.pull.unconsChunk.flatMap {
          case Some((chnk, rest)) =>
            val bytes = chnk
            val (ix, strim) = splitAndIgnorePrev(values, state, bytes)
            go(rest, ix, strim)
          case None =>
            Pull.raiseError(MalformedMessageBodyFailure("Malformed Malformed match"))
        }
      }

    stream.pull.unconsChunk.flatMap {
      case Some((chnk, strim)) =>
        val (ix, rest) = splitAndIgnorePrev(values, 0, chnk)
        go(strim, ix, rest)
      case None =>
        Pull.raiseError(MalformedMessageBodyFailure("Cannot parse empty stream"))
    }.stream
  }

  /**
    *
    * @param boundary
    * @param s
    * @param limit
    * @tparam F
    * @return
    */
  private def pullParts[F[_]: Sync](
    boundary: Boundary,
    s: Stream[F, Byte],
    limit: Int
  ): Pull[F, Part[F], Unit] = {
    val values = DoubleCRLFBytesN
    val expectedBytes = ExpectedBytesN(boundary)

    splitOrFinish[F](values, s, limit).flatMap {
      case (l, r) =>
        //We can abuse reference equality here for efficiency
        //Since `splitOrFinish` returns `empty` on a capped stream
        //However, we must have at least one part, so `splitOrFinish` on this function
        //Indicates an error
        if (r == streamEmpty) {
          Pull.raiseError(MalformedMessageBodyFailure("Cannot parse empty stream"))
        } else {
          tailrecParts[F](boundary, l, r, expectedBytes, limit)
        }
    }
  }

  private def tailrecParts[F[_]: Sync](
    b: Boundary,
    headerStream: Stream[F, Byte],
    rest: Stream[F, Byte],
    expectedBytes: Array[Byte],
    limit: Int): Pull[F, Part[F], Unit] =
    Pull
      .eval(parseHeaders(headerStream))
      .flatMap { hdrs =>
        splitHalf(expectedBytes, rest).flatMap {
          case (l, r) =>
            //We hit a boundary, but the rest of the stream is empty
            //and thus it's not a properly capped multipart body
            if (r == streamEmpty) {
              Pull.raiseError(MalformedMessageBodyFailure("Part not terminated properly"))
            } else {
              Pull.output1(Part[F](hdrs, l)) >> splitOrFinish(DoubleCRLFBytesN, r, limit).flatMap {
                case (hdrStream, remaining) =>
                  if (hdrStream == streamEmpty) { //Empty returned if it worked fine
                    Pull.done
                  } else {
                    tailrecParts[F](b, hdrStream, remaining, expectedBytes, limit)
                  }
              }
            }
        }
      }

  /** Split a stream in half based on `values`,
    * but check if it is either double dash terminated (end of multipart).
    * SplitOrFinish also tracks a header limit size
    *
    * If it is, return the empty stream. if it is not, split on the `values`
    * and raise an error if we lack a match
    */
  //noinspection ScalaStyle
  private def splitOrFinish[F[_]: Sync](
    values: Array[Byte],
    stream: Stream[F, Byte],
    limit: Int): SplitStream[F] = {

    /** Check if a particular chunk a final chunk, that is,
      * whether it's the boundary plus an extra "--", indicating it's
      * the last boundary
      */
    def checkIfLast(c: Chunk[Byte], rest: Stream[F, Byte]): SplitStream[F] =
      if (c.size <= 0) {
        Pull.raiseError(MalformedMessageBodyFailure("Invalid Chunk: Chunk is empty"))
      } else if (c.size == 1) {
        rest.pull.unconsChunk.flatMap {
          case Some((chnk, remaining)) =>
            if (chnk.size <= 0)
              Pull.raiseError(MalformedMessageBodyFailure("Invalid Chunk: Chunk is empty"))
            else if (c(0) == dashByte && chnk(0) == dashByte) {
              Pull.pure((streamEmpty, streamEmpty))
            } else {
              val (ix, l, r, add) =
                splitOnChunkLimited[F](
                  values,
                  0,
                  Chunk.bytes(c.toArray[Byte] ++ chnk.toArray[Byte]),
                  Stream.empty,
                  Stream.empty)
              go(remaining, ix, l, r, add)
            }
          case None =>
            Pull.raiseError(MalformedMessageBodyFailure("Malformed Multipart ending"))
        }
      } else if (c(0) == dashByte && c(1) == dashByte) {
        Pull.pure((streamEmpty, streamEmpty))
      } else {
        val (ix, l, r, add) =
          splitOnChunkLimited[F](values, 0, c, Stream.empty, Stream.empty)
        go(rest, ix, l, r, add)
      }

    def go(
      s: Stream[F, Byte],
      state: Int,
      lacc: Stream[F, Byte],
      racc: Stream[F, Byte],
      limitCTR: Int): SplitStream[F] =
      if (limitCTR >= limit) {
        Pull.raiseError(
          MalformedMessageBodyFailure(s"Part header was longer than $limit-byte limit"))
      } else if (state == values.length) {
        Pull.pure((lacc, racc ++ s))
      } else {
        s.pull.unconsChunk.flatMap {
          case Some((chnk, str)) =>
            val (ix, l, r, add) = splitOnChunkLimited[F](values, state, chnk, lacc, racc)
            go(str, ix, l, r, limitCTR + add)
          case None =>
            Pull.raiseError(MalformedMessageBodyFailure("Invalid boundary - partial boundary"))
        }
      }

    stream.pull.unconsChunk.flatMap {
      case Some((chunk, rest)) =>
        checkIfLast(chunk, rest)
      case None =>
        Pull.raiseError(MalformedMessageBodyFailure("Invalid boundary - partial boundary"))
    }
  }

  /** Take the stream of headers separated by
    * double CRLF bytes and return the headers
    */
  private def parseHeaders[F[_]: Sync](strim: Stream[F, Byte]): F[Headers] = {
    def tailrecParse(s: Stream[F, Byte], headers: Headers): Pull[F, Headers, Unit] =
      splitHalf[F](CRLFBytesN, s).flatMap {
        case (l, r) =>
          l.through(fs2.text.utf8Decode[F])
            .fold("")(_ ++ _)
            .map { string =>
              val ix = string.indexOf(':')
              if (string.indexOf(':') >= 0) {
                headers.put(Header(string.substring(0, ix), string.substring(ix + 1).trim))
              } else {
                headers
              }
            }
            .pull
            .echo >> r.pull.uncons.flatMap {
            case Some(_) =>
              tailrecParse(r, headers)
            case None =>
              Pull.done
          }
      }

    tailrecParse(strim, Headers.empty).stream.compile
      .fold(Headers.empty)(_ ++ _)
  }

  /** Spit our `Stream[F, Byte]` into two halves.
    * If we reach the end and the state is 0 (meaning we didn't match at all),
    * then we return the concatenated parts of the stream.
    *
    * This method _always_ caps
    */
  private def splitHalf[F[_]: Sync](
    values: Array[Byte],
    stream: Stream[F, Byte]): SplitStream[F] = {

    def go(
      s: Stream[F, Byte],
      state: Int,
      lacc: Stream[F, Byte],
      racc: Stream[F, Byte]): SplitStream[F] =
      if (state == values.length) {
        Pull.pure((lacc, racc ++ s))
      } else {
        s.pull.unconsChunk.flatMap {
          case Some((chnk, str)) =>
            val (ix, l, r) = splitOnChunk[F](values, state, chnk, lacc, racc)
            go(str, ix, l, r)
          case None =>
            //We got to the end, and matched on nothing.
            Pull.pure((lacc ++ racc, streamEmpty))
        }
      }

    stream.pull.unconsChunk.flatMap {
      case Some((chunk, rest)) =>
        val (ix, l, r) = splitOnChunk[F](values, 0, chunk, Stream.empty, Stream.empty)
        go(rest, ix, l, r)
      case None =>
        Pull.pure((streamEmpty, streamEmpty))
    }
  }

  /** Split a chunk in the case of a complete match:
    *
    * If it is a chunk that is between a partial match
    * (middleChunked), consider the prior partial match
    * as part of the data to emit.
    *
    * If it is a fully matched, fresh chunk (no carry over partial match),
    * emit everything until the match, and everything after the match.
    *
    * If it is the continuation of a partial match,
    * emit everything after the partial match.
    *
    */
  private def splitCompleteLimited[F[_]: Sync](
    state: Int,
    middleChunked: Boolean,
    sti: Int,
    i: Int,
    acc: Stream[F, Byte],
    carry: Stream[F, Byte],
    c: Chunk[Byte]
  ): (Int, Stream[F, Byte], Stream[F, Byte], Int) =
    if (middleChunked) {
      (
        sti,
        //Emit the partial match as well
        acc ++ carry ++ Stream.chunk(c.take(i - sti)),
        //Emit after the match
        Stream.chunk(c.drop(i)),
        state + i - sti)
    } else {
      (
        sti,
        acc, //block completes partial match, so do not emit carry
        Stream.chunk(c.drop(i)), //Emit everything after the match
        0)
    }

  /** Split a chunk in the case of a partial match:
    *
    * If it is a chunk that is between a partial match
    * (middle chunked), the prior partial match is added to
    * the accumulator, and the current partial match is
    * considered to carry over.
    *
    * If it is a fresh chunk (no carry over partial match),
    * everything prior to the partial match is added to the accumulator,
    * and the partial match is considered the carry over.
    *
    * Else, if the whole block is a partial match,
    * add it to the carry over
    *
    */
  private def splitPartialLimited[F[_]: Sync](
    state: Int,
    middleChunked: Boolean,
    currState: Int,
    i: Int,
    acc: Stream[F, Byte],
    carry: Stream[F, Byte],
    c: Chunk[Byte]
  ): (Int, Stream[F, Byte], Stream[F, Byte], Int) = {
    val ixx = i - currState
    if (middleChunked) {
      val (lchunk, rchunk) = c.splitAt(ixx)
      (
        currState,
        acc ++ carry ++ Stream.chunk(lchunk), //Emit previous carry
        Stream.chunk(rchunk),
        state + ixx)
    } else {
      //Whole thing is partial match
      (currState, acc, carry ++ Stream.chunk(c), 0)
    }
  }

  private def splitOnChunkLimited[F[_]: Sync](
    values: Array[Byte],
    state: Int,
    c: Chunk[Byte],
    acc: Stream[F, Byte],
    carry: Stream[F, Byte]): (Int, Stream[F, Byte], Stream[F, Byte], Int) = {
    var i = 0
    var currState = state
    val len = values.length
    while (currState < len && i < c.size) {
      if (c(i) == values(currState)) {
        currState += 1
      } else if (c(i) == values(0)) {
        currState = 1
      } else {
        currState = 0
      }
      i += 1
    }

    //It will only be zero if
    //the chunk matches from the very beginning,
    //since currstate can never be greater than
    //(i + state).
    val middleChunked = i + state - currState > 0

    if (currState == 0) {
      (0, acc ++ carry ++ Stream.chunk(c), Stream.empty, i)
    } else if (currState == len) {
      splitCompleteLimited(state, middleChunked, currState, i, acc, carry, c)
    } else {
      splitPartialLimited(state, middleChunked, currState, i, acc, carry, c)
    }
  }

}
