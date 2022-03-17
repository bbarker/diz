import java.io.IOException

import discord4j.common.close.CloseException
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.{
  DiscordClient,
  DiscordClientBuilder,
  GatewayDiscordClient
}
import discord4j.rest.http.client.ClientException
import io.github.bbarker.diz.data.*
import org.reactivestreams.Publisher
import reactor.core.publisher.{Flux, Mono}
import zio.Console.*
import zio.Exit.Success
import zio.*
import zio.interop.reactivestreams.*
import zio.stream.*

object EnvVars:
  val discordToken = "DISCORD_TOKEN"

object Diz extends zio.App:
  def run(args: List[String]) =
    mainLogic
      .retryWhileM {
        case ex: ClientException => warnError(ex) *> UIO(true)
        case ex: CloseException  => warnError(ex) *> UIO(true)
        case _                   => UIO(false)
      }
      .catchAll(err => putStrLn(s"Error: $err"))
      .catchAllDefect(err => putStrLn(s"Defect: $err"))
      .exitCode

  def warnError(err: => Any)(implicit
      trace: ZTraceElement
  ): URIO[Console, Unit] =
    putStrLn(s"Restarting due to Error: $err").orDie

  val mainLogic: ZIO[Console, Throwable, Unit] =
    (for {
      _ <- putStrLn("Starting DiZ bot")
      discordToken <- ZIO
        .fromOption(sys.env.get(EnvVars.discordToken))
        .mapError(err =>
          new RuntimeException(s"${EnvVars.discordToken} not set")
        )
      client <- UIO(DiscordClientBuilder.create(discordToken).build())
      gateway <- ZIO.effect(client.login.block())
      _ <- mainStream(gateway).runDrain
      _ <- ZIO.effect(gateway.onDisconnect().block())

    } yield ()).provideSomeLayer(DizQuotes.layer ++ Random.live)

  def mainStream(
      gateway: GatewayDiscordClient
  ): ZStream[Quotes & Random, Throwable, Unit] = for {
    userMessage <- getUserMessages(gateway)
    botMessage <- getBotMessages(gateway)
    _ <- ZStream.mergeAllUnbounded()(
      mainUserMessageStream(userMessage),
      mainBotMessageStream(botMessage)
    )
  } yield ()

  def mainUserMessageStream(
      userMessage: Message
  ): ZStream[Quotes & Random, Throwable, Unit] =
    UStream(userMessage).flatMap(msg =>
      ZStream.mergeAllUnbounded()(
        randomlySayQuote(msg, maxQuoteRoll = 25),
        pingPong(msg).map(_ => ())
        // Keep pingPong last to make sure streams are being evaluated correctly
      )
    )

  def mainBotMessageStream(
      botMessage: Message
  ): ZStream[Quotes & Random, Throwable, Unit] =
    UStream(botMessage).flatMap(msg =>
      ZStream.mergeAllUnbounded()(
        snarkOnRoll(msg)
      )
    )

  def pingPong(
      userMessage: Message
  ): Stream[Throwable, Message] = userMessage match
    case msg if msg.getContent.equalsIgnoreCase("!ping") =>
      userMessage.getChannel
        .toStream()
        .flatMap(channel => channel.createMessage("Pong!").toStream())
    case _ => ZStream.empty

  /** Will only say a quote when the the max quote roll is rolled
    */
  def randomlySayQuote(
      userMessage: Message,
      maxQuoteRoll: Int
  ): ZStream[Quotes & Random, Throwable, Unit] = for {
    // TODO: need to integrate Flux and ZIO, see #1
    // roll <- Random.nextIntBetween(1, maxQuoteRoll + 1)
    quotes <- ZStream.service[Quotes]
    roll <- ZStream.fromEffect(Random.nextIntBetween(1, maxQuoteRoll + 1))
    _ <- roll match
      case r if r == maxQuoteRoll =>
        val bestQuote =
          quotes.sayQuote(quotes.findBestQuote(userMessage.getContent()))
        bestQuote match
          case Some(quote) =>
            userMessage.getChannel.flatMap(_.createMessage(quote)).toStream()
          case None => ZStream.empty
      case _ => ZStream.empty
  } yield ()

  def snarkOnRoll(userMessage: Message): ZStream[Random, Throwable, Unit] =
    for {
      _ <- ZStream.fromEffect(ZIO.debug(userMessage))
      _ <- ZStream.fromEffect(
        ZIO.debug(s"above userMessage content: ${userMessage.getContent}")
      )
    } yield ()

  def getUserMessages(
      gateway: GatewayDiscordClient
  ): Stream[Throwable, Message] =
    (gateway
      .getEventDispatcher()
      .on(classOf[MessageCreateEvent]): Publisher[MessageCreateEvent])
      .toStream()
      .map(_.getMessage)
      .filter(message =>
        message.getAuthor().map(user => !user.isBot()).orElse(false)
      )

  def getBotMessages(
      gateway: GatewayDiscordClient
  ): Stream[Throwable, Message] =
    (gateway
      .getEventDispatcher()
      .on(classOf[MessageCreateEvent]): Publisher[MessageCreateEvent])
      .toStream()
      .map(_.getMessage)
      .filter(message =>
        message.getAuthor().map(user => user.isBot()).orElse(false)
      )
