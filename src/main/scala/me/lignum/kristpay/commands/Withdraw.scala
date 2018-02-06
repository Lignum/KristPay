package me.lignum.kristpay.commands

import me.lignum.kristpay.KristPay
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments._
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.cause.{Cause, EventContext, EventContextKeys}
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
        val taxAmount = if (KristPay.get.config.taxes.enabled) {
          Math.floor(Math.max(1.0, amount.toDouble * KristPay.get.config.taxes.withdrawMultiplier)).toInt
        } else {
          0
        }

        val taxedAmount = amount - taxAmount

        val minimumAmount = if (KristPay.get.config.taxes.enabled) {
          Math.floor(1.0 / (1.0 - KristPay.get.config.taxes.withdrawMultiplier)).toInt
        } else {
          1
        }

        if (amount < 1 || (amount >= 0 && taxedAmount < 1)) {
          src.sendMessage(
            Text.builder("You need to withdraw at least " + minimumAmount + " KST.")
              .color(TextColors.RED)
              .build()
          )

          return CommandResult.success()
        }

        if (!address.matches("^(?:[a-f0-9]{10}|k[a-z0-9]{9}|(?:[a-z0-9-_]{1,32}@)?[a-z0-9]{1,64}\\.kst)$")) {
          src.sendMessage(
            Text.builder("\"" + address + "\" is not a valid Krist address!")
              .color(TextColors.RED)
              .build()
          )

          CommandResult.success()
        } else {
          val economy = KristPay.get.economyService
          val uuid = player.getUniqueId

          val accountOpt = economy.getOrCreateAccount(uuid)

          if (accountOpt.isPresent) {
            val account = accountOpt.get
            val result = account.withdraw(
              KristPay.get.currency, java.math.BigDecimal.valueOf(amount), Cause.of(EventContext.builder()
                .add(EventContextKeys.PLAYER_SIMULATED, player.getProfile)
                .build(), this)
            )

            result.getResult match {
              case ResultType.SUCCESS =>
                val master = KristPay.get.masterWallet

                master.transfer(address, taxedAmount, {
                  case Some(ok) =>
                    if (ok) {
                      val withdrawMsg = if (KristPay.get.config.taxes.enabled) {
                        "Successfully withdrawn " + taxedAmount + " KST (" + amount + " KST - " + taxAmount + " KST tax)."
                      } else {
                        "Successfully withdrawn " + taxedAmount + " KST."
                      }

                      src.sendMessage(
                        Text.builder(withdrawMsg)
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
                        KristPay.get.currency, java.math.BigDecimal.valueOf(amount),
                        Cause.of(EventContext.empty(), this), null
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
                      KristPay.get.currency, java.math.BigDecimal.valueOf(amount),
                      Cause.of(EventContext.empty(), this), null
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
