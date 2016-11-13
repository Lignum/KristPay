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

object Pay {
  val spec = CommandSpec.builder()
    .description(Text.of("Transfers money from you to another player."))
    .permission("kristpay.command.pay")
    .arguments(
      onlyOne(player(Text.of("target"))),
      onlyOne(integer(Text.of("amount")))
    )
    .executor(new Pay)
    .build()
}

class Pay extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext) = src match {
    case player: Player =>
      val targetOpt = args.getOne[Player]("target")
      val amountOpt = args.getOne[Int]("amount")

      if (!targetOpt.isPresent || !amountOpt.isPresent) {
        src.sendMessage(
          Text.builder("Usage: /pay <player> <amount>")
            .color(TextColors.RED)
            .build()
        )

        CommandResult.success()
      } else {
        val target = targetOpt.get
        val amount = amountOpt.get

        if (amount < 0) {
          src.sendMessage(
            Text.builder("You can't make a negative transaction!")
              .color(TextColors.RED)
              .build()
          )

          CommandResult.success()
        } else {
          val economy = KristPay.get.economyService
          val theirAccount = economy.getOrCreateAccount(target.getUniqueId)
          val ourAccount = economy.getOrCreateAccount(player.getUniqueId)

          if (theirAccount.isPresent && ourAccount.isPresent) {
            val theirs = theirAccount.get()
            val ours = ourAccount.get()

            val result = ours.transfer(
              theirs, KristPay.get.currency, java.math.BigDecimal.valueOf(amount),
              Cause.of(NamedCause.simulated(player)), null
            )

            result.getResult match {
              case ResultType.SUCCESS =>
                val amountTransferred = result.getAmount.intValue

                src.sendMessage(
                  Text.of(TextColors.GREEN, "Successfully transferred " + amountTransferred + " KST to " + target.getName + ".")
                )

                target.sendMessage(
                  Text.of(TextColors.GREEN, "You have received " + amountTransferred + " KST from " + player.getName)
                )

              case ResultType.ACCOUNT_NO_FUNDS =>
                src.sendMessage(
                  Text.builder("You do not have enough funds for this transaction!")
                    .color(TextColors.RED)
                    .build()
                )

              case _ =>
                src.sendMessage(
                  Text.builder("Transaction failed with result " + result.getResult.toString + ".")
                    .color(TextColors.RED)
                    .build()
                )
            }

            CommandResult.success()
          } else {
            src.sendMessage(
              Text.builder("Failed to find your account. Sorry!")
                .color(TextColors.RED)
                .build()
            )

            CommandResult.success()
          }
        }
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
