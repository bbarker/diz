package io.github.bbarker.diz.data

import zio.*
import zio.test.Assertion.*
import zio.test.*

object DizQuotesSpec extends zio.test.ZIOSpecDefault:

  val theGlovesQuote: String = "The gloves must come off then."
  override def spec: Spec[Environment, TestSuccess] =
    suite("DizQuotesSpec")(
      test("Reads data from resource file") {
        for {
          dizQuotes <- ZIO.service[DizQuotes]
        } yield assert(dizQuotes.quotes.toIterable)(contains(theGlovesQuote))
      }
    ).provideSomeLayer[Environment](DizQuotes.layer)
