package io.github.bbarker.diz.data

import scala.util.matching.Regex

import zio.*

case class DizQuotes(quotes: Array[String]) extends Quotes:
  def name = Some("DiZ")

object DizQuotes:
  val quotePattern: Regex = "\"''(.*)''\"".r

  val layer: ULayer[DizQuotes] = ZLayer.fromEffect(for {
    _ <- UIO(()) // TODO: read from resources
    lines = "???".split("\n")
    quotes = lines.flatMap(line =>
      quotePattern.findAllMatchIn(line).map(_.group(1)).toArray
    )

  } yield DizQuotes(quotes))
