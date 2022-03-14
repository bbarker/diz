import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.{
  DiscordClient,
  DiscordClientBuilder,
  GatewayDiscordClient
}
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
      .catchAll(err => putStrLn(s"Error: $err"))
      .catchAllDefect(err => putStrLn(s"Defect: $err"))
      .exitCode

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
      userMessages = getUserMessages(gateway)

      _ <- pingPong(
        userMessages
      ).runDrain // FIXME: test to see if putting this here first works, which is the case
      // FIXME: need to multicast
      _ <- randomlySayQuote(userMessages, maxQuoteRoll = 25).runDrain
      _ <- ZIO.effect(gateway.onDisconnect().block())

    } yield ()).provideSomeLayer(DizQuotes.layer ++ Random.live)

  def pingPong(
      userMessages: Stream[Throwable, Message]
  ): Stream[Throwable, Message] =
    userMessages
      .filter(message => message.getContent().equalsIgnoreCase("!ping"))
      .flatMap(_.getChannel.toStream())
      .flatMap(channel => channel.createMessage("Pong!").toStream())

  /** Will only say a quote when the the max quote roll is rolled
    */
  def randomlySayQuote(
      userMessages: Stream[Throwable, Message],
      maxQuoteRoll: Int
  ): ZStream[Quotes & Random & Console, Throwable, Unit] = for {
    // TODO: need to integrate Flux and ZIO, see #1
    // roll <- Random.nextIntBetween(1, maxQuoteRoll + 1)
    quotes <- ZStream.service[Quotes]
    _ <- userMessages
      .flatMap(message => {
        val roll = Runtime.default
          .unsafeRunSync(Random.nextIntBetween(1, maxQuoteRoll + 1))
        roll match
          case r if r == Success(maxQuoteRoll) =>
            val bestQuote =
              quotes.sayQuote(quotes.findBestQuote(message.getContent()))
            bestQuote match
              case Some(quote) =>
                message.getChannel.flatMap(_.createMessage(quote)).toStream()
              case None => ZStream.empty
          case _ => ZStream.empty
      })
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
