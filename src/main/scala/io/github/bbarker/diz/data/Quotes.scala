package io.github.bbarker.diz.data

import scala.util.matching.Regex

import zio.*

trait Quotes:
  // Who says the quote
  def name: Option[String]

  def quotes: Array[String]

  def rules: List[(String, String) => Boolean] =
    List((msg, qt) => qt.length <= (2 * msg.length))

  def evalRules(message: String)(quote: String): Boolean =
    rules.forall(f => f(message, quote))

  def sayQuote(quote: Option[String]): Option[String] = name match
    case Some(nm) => quote.map(qt => s"$nm says \"$qt\"")
    case None     => quote

  def quoteOverlap(message: String)(quote: String): Int =
    evalRules(message)(quote) match
      case true =>
        val regex = new Regex("""(?i)\b(?:[a-z]{2,})\b""")
        val wordsInMessage = regex.findAllIn(message).toSet
        val wordsInQuote = regex.findAllIn(quote).toSet
        wordsInMessage.intersect(wordsInQuote).size
      case false => 0

  def findBestQuote(message: String): Option[String] =
    val quoteOverlaps = quotes.map(quoteOverlap(message))
    val maxOverlap = quoteOverlaps.max
    if (maxOverlap > 0) then
      val bestQuotes =
        quoteOverlaps.zip(quotes).filter(_._1 == maxOverlap).map(_._2)
      bestQuotes.headOption
    else None

object Quotes:
  def apply(quotes: List[Quotes]): Quotes = new Quotes {
    // TODO: run findBestQuote for each Quote object, then run again on the resulting quotes
    def name = ???
    def quotes = ???
  }
