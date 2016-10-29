package me.lignum.kristpay.commands

import me.lignum.kristpay.KristPayPlugin
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments._
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.cause.{Cause, NamedCause}
import org.spongepowered.api.service.economy.transaction.ResultType
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object Withdraw {
  val spec = CommandSpec.builder()
    .description(Text.of("Sends your server balance to your actual Krist wallet."))
    .permission("kristpay.command.withdraw")
    .arguments(
      onlyOne(string(Text.of("address"))),
      onlyOne(integer(Text.of("amount")))
    )
    .executor(new Withdraw)
    .build()
}

class Withdraw extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = src match {
    case player: Player =>
      val addressOpt = args.getOne[String]("address")
      val amountOpt = args.getOne[Int]("amount")

      if (!addressOpt.isPresent || !amountOpt.isPresent) {
        src.sendMessage(
          Text.builder("Usage: /withdraw <address> <amount>")
            .color(TextColors.RED)
            .build()
        )

        CommandResult.success()
      } else {
        val address = addressOpt.get()
        val amount = amountOpt.get()

        if (!address.matches("^(?:[a-f0-9]{10}|k[a-z0-9]{9})$")) {
          src.sendMessage(
            Text.builder("\"" + address + "\" is not a valid Krist address!")
              .color(TextColors.RED)
              .build()
          )

          CommandResult.success()
        } else {
          val economy = KristPayPlugin.get.economyService
          val uuid = player.getUniqueId

          val accountOpt = economy.getOrCreateAccount(uuid)

          if (accountOpt.isPresent) {
            val account = accountOpt.get
            val result = account.withdraw(
              KristPayPlugin.get.currency, java.math.BigDecimal.valueOf(amount), Cause.of(NamedCause.simulated(player))
            )

            result.getResult match {
              case ResultType.SUCCESS =>
                val master = KristPayPlugin.get.masterWallet

                master.transfer(address, amount, {
                  case Some(ok) =>
                    if (ok) {
                      src.sendMessage(
                        Text.builder("Successfully withdrawn " + result.getAmount + " KST!")
                            .color(TextColors.GREEN)
                            .build()
                      )
                    } else {
                      src.sendMessage(
                        Text.builder("Transaction failed. Perhaps the master wallet is exhausted?")
                          .color(TextColors.RED)
                          .build()
                      )

                      // Refund
                      account.deposit(
                        KristPayPlugin.get.currency, java.math.BigDecimal.valueOf(amount),
                        Cause.of(NamedCause.source(this)), null
                      )
                    }

                  case None =>
                    src.sendMessage(
                      Text.builder("Transaction failed. Perhaps Krist is down right now?")
                        .color(TextColors.RED)
                        .build()
                    )

                    // Refund
                    account.deposit(
                      KristPayPlugin.get.currency, java.math.BigDecimal.valueOf(amount),
                      Cause.of(NamedCause.source(this)), null
                    )
                })

                src.sendMessage(
                  Text.builder("Withdraw requested... Just a sec!")
                    .color(TextColors.GREEN)
                    .build()
                )

              case ResultType.ACCOUNT_NO_FUNDS =>
                src.sendMessage(
                  Text.builder("You don't have enough funds to withdraw that much!")
                    .color(TextColors.RED)
                    .build()
                )

              case _ =>
                src.sendMessage(
                  Text.builder("Transaction failed!")
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
