import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

import zio.*
import zio.Console.*


val discordTokenVar = "DISCORD_TOKEN"


object Diz extends zio.App:
  def run(args: List[String]) =
    println("starting myAppLogic")
    myAppLogic.exitCode

  val myAppLogic =
    for {
      _ <- putStrLn("starting myAppLogic 2")
      // discordToken <- ZIO.fromOption(sys.env.get(discordTokenVar))

      _    <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _    <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
    } yield ()

  /*

  public final class ExampleBot {

  public static void main(final String[] args) {
    final String token = args[0];
    final DiscordClient client = DiscordClient.create(token);
    final GatewayDiscordClient gateway = client.login().block();

    gateway.on(MessageCreateEvent.class).subscribe(event -> {
      final Message message = event.getMessage();
      if ("!ping".equals(message.getContent())) {
        final MessageChannel channel = message.getChannel().block();
        channel.createMessage("Pong!").block();
      }
    });

    gateway.onDisconnect().block();
  }
}
*/
