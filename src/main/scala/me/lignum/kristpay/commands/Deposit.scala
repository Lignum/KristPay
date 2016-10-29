package me.lignum.kristpay.commands

import me.lignum.kristpay.KristPayPlugin
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object Deposit {
  val spec = CommandSpec.builder()
    .description(Text.of("Gives you your deposit address."))
    .permission("kristpay.command.deposit")
    .executor(new Deposit)
    .build()
}

class Deposit extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = src match {
    case player: Player =>
      val uuid = player.getUniqueId

      KristPayPlugin.get.database.accounts.find(_.owner.equals(uuid.toString)) match {
        case Some(acc) =>
          src.sendMessage(
            Text.builder(
              "Your deposit address is \"" + acc.depositWallet.address + "\"." +
                (
                  if (KristPayPlugin.get.database.floatingFunds.enabled) {
                    " You will not receive deposits instantly. Use /payout to see the next payout time."
                  } else {
                    ""
                  }
                ))
              .color(TextColors.GREEN)
              .build()
          )

          CommandResult.success()

        case None =>
          src.sendMessage(
            Text.builder("You do not have an account. Use /balance to create one.")
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
