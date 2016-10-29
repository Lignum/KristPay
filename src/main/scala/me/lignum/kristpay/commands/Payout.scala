package me.lignum.kristpay.commands

import me.lignum.kristpay.KristPayPlugin
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object Payout {
  val spec = CommandSpec.builder()
    .description(Text.of("Shows the next payout time."))
    .permission("kristpay.command.payout")
    .executor(new Payout)
    .build()
}

class Payout extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = {
    val nextTime =
      KristPayPlugin.get.nextPayoutTime / 1000L - System.currentTimeMillis / 1000L

    src.sendMessage(
      Text.builder("The next payout will be in " + nextTime + "s.")
        .color(TextColors.GREEN)
        .build()
    )

    CommandResult.builder()
      .queryResult(nextTime.toInt)
      .build()
  }
}
