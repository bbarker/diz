package io.github.bbarker.diz.data

import zio.*
import zio.test.Assertion.*
import zio.test.*

object QuotesSpec extends ZIOSpecDefault:

  val testGlovesMessage: String = "Give me the gloves then."
  val theGlovesQuote: String = "The gloves must come off then."

  val testQuotes = new Quotes:
    val name = Some("TestQuotes")
    val quotes = Array(theGlovesQuote, "foo", "bar baz", "gloves")

  override def spec: ZSpec[Environment, TestSuccess] =
    suite("QuotesSpec")(
      test("Detects overlap") {
        assert(testQuotes.quotes.toIterable)(contains(theGlovesQuote))
      },
      test("Finds the best quote") {
        assert(testQuotes.findBestQuote(testGlovesMessage).toIterable)(
          contains(theGlovesQuote)
        )
        && assert(testQuotes.findBestQuote("foo bar baz").toIterable)(
          contains("bar baz")
        )
        && assert(testQuotes.findBestQuote("fee fi foe foo").toIterable)(
          contains("foo")
        )
        && assert(testQuotes.findBestQuote("Joe Shmoe"))(
          equalTo(None)
        )
        && assert(testQuotes.findBestQuote(""))(
          equalTo(None)
        )
      }
    )
