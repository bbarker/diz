package io.github.bbarker.diz.data

import scala.util.matching.Regex

import zio.*
import zio.prelude.Reader

trait Quotes {
  // Who says the quote
  def name: Option[String]

  def quotes: Array[String]

  def quoteOverlap(message: String)(quote: String): Int = {
    val regex = new Regex("""(?i)\b(?:[a-z]{2,})\b""")
    val wordsInMessage = regex.findAllIn(message).toSet
    val wordsInQuote = regex.findAllIn(quote).toSet
    wordsInMessage.intersect(wordsInQuote).size
  }

  def findBestQuote(message: String): Option[String] = {
    val quoteOverlaps = quotes.map(quoteOverlap(message))
    val maxOverlap = quoteOverlaps.max
    val bestQuotes =
      quoteOverlaps.zip(quotes).filter(_._1 == maxOverlap).map(_._2)
    bestQuotes.headOption
  }

}
object Quotes {
  def apply(quotes: List[Quotes]): Quotes = new Quotes {
    // TODO: run findBestQuote for each Quote object, then run again on the resulting quotes
    def name = ???
    def quotes = ???
  }
}

object DizQuotes extends Quotes {
  def name = Some("DiZ")

  val quotePattern: Regex = "\"''(.*)''\"".r

  val lines: Array[String] = "???".split("\n")

  val quotes: Array[String] =
    lines.flatMap(line =>
      quotePattern.findAllMatchIn(line).map(_.group(1)).toArray
    )

}
