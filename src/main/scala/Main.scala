import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.{DiscordClient, DiscordClientBuilder, GatewayDiscordClient}
import zio.Console.*
import zio.*

object EnvVars:
  val discordToken = "DISCORD_TOKEN"

object Diz extends zio.App:
  def run(args: List[String]) =
    println("starting myAppLogic")
    mainLogic
      .catchAll(err => putStrLn(s"Error: $err"))
      .catchAllDefect(err => putStrLn(s"Defect: $err"))
      .exitCode

  val mainLogic: ZIO[Console, Throwable, Unit] =
    for {
      _ <- putStrLn("starting myAppLogic 2")
      discordToken <- ZIO
        .fromOption(sys.env.get(EnvVars.discordToken))
        .mapError(err =>
          new RuntimeException(s"${EnvVars.discordToken} not set")
        )
      client <- UIO(DiscordClientBuilder.create(discordToken).build())
      gateway <- ZIO.effect(client.login.block())

      _ <- pingPong(gateway)
      _ <- ZIO.effect(gateway.onDisconnect().block())

    } yield ()

  def pingPong(gateway: GatewayDiscordClient): Task[Unit] = ZIO.effect(
    gateway
      .getEventDispatcher()
      .on(classOf[MessageCreateEvent])
      .map(_.getMessage)
      .filter(message =>
        message.getAuthor().map(user => !user.isBot()).orElse(false)
      )
      .filter(message => message.getContent().equalsIgnoreCase("!ping"))
      .flatMap(_.getChannel)
      .flatMap(channel => channel.createMessage("Pong!"))
      .subscribe()
  )
