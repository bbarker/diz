package io.github.bbarker.diz.data

import scala.util.matching.Regex

import zio.*

case class DizQuotes(quotes: Array[String]) extends Quotes:
  def name = Some("DiZ")

object DizQuotes:
  val quotePattern: Regex = "\"''(.*)''\"".r

  val layer: ULayer[DizQuotes] = ZLayer.fromZIO(for {
    quoteFile <- ZIO
      .attempt(
        scala.io.Source.fromResource("data/dizquotes.txt")(scala.io.Codec.UTF8)
      )
      .orDie
    lines <- ZIO.attempt(quoteFile.getLines.toArray).orDie
    quotes = lines
      .flatMap(line =>
        quotePattern.findAllMatchIn(line).map(_.group(1)).toArray
      )
      .map(_.trim)

  } yield DizQuotes(quotes))
