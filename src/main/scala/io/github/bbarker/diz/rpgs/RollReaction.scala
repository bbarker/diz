package io.github.bbarker.diz.rpgs

import discord4j.core.`object`.entity.Message
import zio.*
import zio.interop.reactivestreams.*
import zio.stream.*

object RollReaction:

  enum RollResult:
    case CritFailure(die: Int)
    case CritSuccess(die: Int)
    case NothingSpecial

  def parseMessage(msg: String): RollResult = ???

  /*   Need to be aple to parse messages like this:
   *
   *     **Result**: 2d20 (17, 6) + 1d4 (2) + 2d6 (5, 2) + 3 + 1
   *     **Total**: 36
   */
  def snarkOnRoll(userMessage: Message): ZStream[Random, Throwable, Unit] =
    for {
      _ <- ZStream.fromEffect(ZIO.debug(userMessage))
      _ <- ZStream.fromEffect(
        ZIO.debug(s"above userMessage content: ${userMessage.getContent}")
      )
    } yield ()
