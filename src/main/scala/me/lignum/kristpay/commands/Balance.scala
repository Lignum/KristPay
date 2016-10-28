package me.lignum.kristpay.commands

import me.lignum.kristpay.KristPayPlugin
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object Balance {
  val spec = CommandSpec.builder()
                        .description(Text.of("Shows your balance."))
                        .executor(new Balance)
                        .permission("kristpay.command.balance")
                        .build()
}

class Balance extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = src match {
    case player: Player =>
      val uuid = player.getUniqueId
      val economy = KristPayPlugin.instance.economyService

      val account = economy.getOrCreateAccount(uuid)

      if (account.isPresent) {
        val acc = account.get()
        val balance = acc.getBalance(economy.getDefaultCurrency)

        src.sendMessage(
          Text.builder("You have " + balance.intValue() + " KST.")
              .color(TextColors.GREEN)
              .build()
        )

        CommandResult.builder()
                     .queryResult(balance.intValue())
                     .build()
      } else {
        src.sendMessage(
          Text.builder("Failed to find your account. Sorry!")
              .color(TextColors.RED)
              .build()
        )

        CommandResult.success()
      }
    case _ =>
      src.sendMessage(
        Text.builder("This command can only be used by players!")
          .color(TextColors.RED)
          .build()
      )

      CommandResult.success()
  }
}
