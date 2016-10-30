package me.lignum.kristpay.commands

import me.lignum.kristpay.KristPay
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments._
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.cause.{Cause, NamedCause}
import org.spongepowered.api.service.economy.transaction.ResultType
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object SetBalance {
  val spec = CommandSpec.builder()
    .description(Text.of("Changes a player's balance."))
    .permission("kristpay.command.setbalance")
    .arguments(
      onlyOne(player(Text.of("target"))),
      onlyOne(integer(Text.of("balance")))
    )
    .executor(new SetBalance)
    .build()
}

class SetBalance extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = {
    val targetOpt = args.getOne[Player]("target")
    val balanceOpt = args.getOne[Int]("balance")

    if (!targetOpt.isPresent || !balanceOpt.isPresent) {
      src.sendMessage(
        Text.builder("Usage: /setbalance <player> <balance>")
          .color(TextColors.RED)
          .build()
      )
    } else {
      val target = targetOpt.get()
      val balance = balanceOpt.get()

      if (balance < 0) {
        src.sendMessage(
          Text.builder("The new balance can't be negative!")
            .color(TextColors.RED)
            .build()
        )

        return CommandResult.success()
      }

      val uuid = target.getUniqueId
      val economy = KristPay.instance.economyService

      val account = economy.getOrCreateAccount(uuid)

      if (account.isPresent) {
        val acc = account.get()
        val result = acc.setBalance(
          KristPay.instance.currency, java.math.BigDecimal.valueOf(balance), Cause.of(NamedCause.source(src)), null
        )

        if (result.getResult == ResultType.SUCCESS) {
          src.sendMessage(
            Text.builder("Successfully set the balance of " + target.getName + " to " + balance + ".")
              .color(TextColors.GREEN)
              .build()
          )

          return CommandResult.builder()
            .queryResult(balance)
            .build()
        } else {
          src.sendMessage(
            Text.builder("Could not set the balance. Perhaps the master wallet is exhausted?")
              .color(TextColors.RED)
              .build()
          )
        }
      } else {
        src.sendMessage(
          Text.builder("Failed to find your account. Sorry!")
            .color(TextColors.RED)
            .build()
        )
      }
    }

    CommandResult.success()
  }
}
