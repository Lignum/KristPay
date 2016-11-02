package me.lignum.kristpay.commands

import java.util.UUID

import me.lignum.kristpay.KristPay
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments._
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.event.cause.{Cause, NamedCause}
import org.spongepowered.api.service.economy.transaction.ResultType
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object EconRegister {
  val spec = CommandSpec
    .builder()
    .permission("kristpay.command.econregister")
    .arguments(
      onlyOne(string(Text.of("uuid"))),
      onlyOne(integer(Text.of("balance")))
    )
    .executor(new EconRegister)
    .build()
}

class EconRegister extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = {
    val uuidOpt = args.getOne[String]("uuid")
    val balanceOpt = args.getOne[Int]("balance")

    if (!uuidOpt.isPresent || !balanceOpt.isPresent) {
      return CommandResult.success()
    }

    val uuid = try {
      UUID.fromString(uuidOpt.get())
    } catch {
      case e: IllegalArgumentException =>
        src.sendMessage(Text.of(TextColors.RED, "Malformed UUID!"))
        return CommandResult.success()
    }

    val balance = balanceOpt.get()
    val economy = KristPay.get.economyService

    if (economy.hasAccount(uuid)) {
      src.sendMessage(Text.of(TextColors.RED, "This player already has an account!"))
      return CommandResult.success()
    }

    val accountOpt = economy.getOrCreateAccount(uuid)

    if (!accountOpt.isPresent) {
      src.sendMessage(Text.of(TextColors.RED, "Failed to register account!"))
    } else {
      val account = accountOpt.get()
      val txResult = account.setBalance(
        economy.getDefaultCurrency,
        java.math.BigDecimal.valueOf(balance),
        Cause.of(NamedCause.source(this))
      )

      val result = txResult.getResult

      if (result != ResultType.SUCCESS) {
        src.sendMessage(Text.of(TextColors.YELLOW, "Account registered, but failed to set balance (" + result.toString + ")!"))
        return CommandResult.success()
      }

      src.sendMessage(Text.of(TextColors.GREEN, "Account registered successfully!"))
    }

    CommandResult.success()
  }
}
