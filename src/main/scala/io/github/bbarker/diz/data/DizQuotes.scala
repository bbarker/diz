package io.github.bbarker.diz.data

import scala.util.matching.Regex

import zio.*

case class DizQuotes(quotes: Array[String]) extends Quotes:
  def name = Some("DiZ")

object DizQuotes:
  val quotePattern: Regex = "\"''(.*)''\"".r

  val layer: ULayer[DizQuotes] = ZLayer.fromEffect(for {
    quoteFile <- ZIO
      .effect(
        scala.io.Source.fromResource("data/dizquotes.txt")(scala.io.Codec.UTF8)
      )
      .orDie
    lines <- UIO(quoteFile.getLines.toArray)
    quotes = lines
      .flatMap(line =>
        quotePattern.findAllMatchIn(line).map(_.group(1)).toArray
      )
      .map(_.strip)

  } yield DizQuotes(quotes))
