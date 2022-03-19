package io.github.bbarker.diz.rpgs

import discord4j.core.`object`.entity.Message
import zio.*
import zio.interop.reactivestreams.*
import zio.stream.*

object RollReaction:

  enum RollResult:
    case CritFailure(die: Int)
    case CritSuccess(die: Int)
    case CritFailureX2(die: Int)
    case CritSuccessX2(die: Int)
    case NothingSpecial

  import RollResult.*

  def parseMessage(msg: String): RollResult = msg match
    case mg if mg.contains("1d20 (**1**)")          => CritFailure(20)
    case mg if mg.contains("1d20 (**20**)")         => CritSuccess(20)
    case mg if mg.contains("2d20 (**1**, **1**)")   => CritFailureX2(20)
    case mg if mg.contains("2d20 (**20**, **20**)") => CritSuccessX2(20)
    case mg if mg.contains("1d12 (**1**)")          => CritFailure(12)
    case mg if mg.contains("1d12 (**12**)")         => CritSuccess(12)
    case mg if mg.contains("2d12 (**1**, **1**)")   => CritFailureX2(12)
    case mg if mg.contains("2d12 (**12**, **12**)") => CritSuccessX2(12)
    case _ => NothingSpecial // The absence doesn't invoke the exhaustive check

  def makeSnark(roll: RollResult): Option[String] = roll match
    case CritFailure(_)   => Some("smooth move exlax")
    case CritFailureX2(_) => Some("smooth move exlax")
    case CritSuccess(_)   => Some("You did it!")
    case CritSuccessX2(_) => Some("Your divine presence may turn the tide")
    case NothingSpecial   => None

  def snarkOnRoll(msg: Message): ZStream[Random, Throwable, Unit] =
    for {
      _ <- ZStream.fromEffect(ZIO.debug(msg))
      _ <- ZStream.fromEffect(
        ZIO.debug(s"above msg content: ${msg.getContent}")
      )
      snarkOpt = makeSnark(parseMessage(msg.getContent))
      _ <- snarkOpt match {
        case Some(snark) =>
          msg.getChannel
            .toStream()
            .flatMap(channel => channel.createMessage(snark).toStream())
        case None => ZStream.empty
      }

    } yield ()
