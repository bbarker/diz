import java.io.IOException
import scala.jdk.OptionConverters.*

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
import io.github.bbarker.diz.Types.*
import io.github.bbarker.diz.data.*
import io.github.bbarker.diz.rpgs.RollReaction.snarkOnRoll
import io.github.bbarker.diz.users.Bots
import io.github.bbarker.diz.users.UserOps.*
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
    message <- getMessageStream(gateway)
    userMessageOpt = getUserMessage(message)
    botMessageOpt = getBotMessage(message)
    _ <- ZStream.mergeAllUnbounded()(
      mainUserMessageStream(userMessageOpt),
      mainBotMessageStream(botMessageOpt)
    )
  } yield ()

  def mainUserMessageStream(
      userMessageOpt: Option[Message]
  ): ZStream[Quotes & Random, Throwable, Unit] =
    fromOption(userMessageOpt).flatMap(msg =>
      ZStream.mergeAllUnbounded()(
        randomlySayQuote(msg, maxQuoteRoll = 25),
        correctTypoStream(msg),
        pingPong(msg).map(_ => ())
        // Keep pingPong last to make sure streams are being evaluated correctly
      )
    )

  def mainBotMessageStream(
      botMessageOpt: Option[Message]
  ): ZStream[Quotes & Random, Throwable, Unit] =
    fromOption(botMessageOpt).flatMap(msg =>
      ZStream.mergeAllUnbounded()(
        onUserMessage(Set(Bots.avrae))(snarkOnRoll(msg))(msg)
      )
    )

  def pingPong(
      userMessage: Message
  ): Stream[Throwable, Message] = userMessage match
    case msg if msg.getContent.equalsIgnoreCase("!ping") =>
      userMessage.getChannel
        .toStream()
        .flatMap(channel =>
          channel
            .createMessage("Pong!")
            .toStream()
        )
    case _ => ZStream.empty

  /** Will only say a quote when the the max quote roll is rolled
    */
  def randomlySayQuote(
      userMessage: Message,
      maxQuoteRoll: Int
  ): ZStream[Quotes & Random, Throwable, Unit] = for {
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

  val typosToCorrect: Map[String, String] = Map(
    "Ashoka" -> "Ahsoka",
    "Bullywog" -> "Bullywug"
  )
  //
  def correctTypo(msg: String)(typo: String): Boolean =
    msg.toLowerCase.contains(
      typo.toLowerCase
    ) && !msg.toLowerCase.contains(typosToCorrect(typo).toLowerCase)
  //
  def correctTypoStream(
      userMessage: Message
  ): ZStream[Any, Throwable, Unit] =
    val msgContent = userMessage.getContent
    val typoOpt =
      typosToCorrect.keySet.find(correctTypo(msgContent))

    typoOpt match
      case Some(typo) =>
        val correction = typosToCorrect(typo)
        val response =
          s"I think you mean $correction${userMessage.mentionAuthorPost}"
        userMessage.getChannel
          .flatMap(_.createMessage(response))
          .toStream()
          .map(_ => ())
      case _ => ZStream.empty

  def getMessageStream(
      gateway: GatewayDiscordClient
  ): Stream[Throwable, Message] =
    (gateway
      .getEventDispatcher()
      .on(classOf[MessageCreateEvent]): Publisher[MessageCreateEvent])
      .toStream()
      .map(_.getMessage)

  def getUserMessage(message: Message): Option[Message] =
    message.getAuthor().map(user => !user.isBot()).orElse(false) match
      case true  => Some(message)
      case false => None

  def getBotMessage(message: Message): Option[Message] =
    message.getAuthor().map(user => user.isBot()).orElse(false) match
      case true  => Some(message)
      case false => None

  def onUserMessage[R, E, A](triggerUsers: Set[DiscordTag])(
      stream: => ZStream[R, E, A]
  )(
      msg: Message
  ): ZStream[R, E, A] =
    ZStream.when(
      msg.getAuthor.toScala.exists(author =>
        triggerUsers.contains(author.getTag)
      )
    )(stream)

  // TODO: see if this can be added as an operator in ZIO
  def fromOption[A](opt: Option[A]): UStream[A] = opt match
    case Some(a) => UStream(a)
    case None    => UStream.empty
