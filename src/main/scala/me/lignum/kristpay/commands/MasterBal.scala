package me.lignum.kristpay.commands

import me.lignum.kristpay.KristPay
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object MasterBal {
  val spec = CommandSpec.builder()
    .description(Text.of("Shows the master wallet's balance."))
    .permission("kristpay.command.masterbal")
    .executor(new MasterBal)
    .build()
}

class MasterBal extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = {
    val masterBal = KristPay.get.masterWallet.balance
    val allocated = KristPay.get.database.getTotalDistributedKrist
    val allocPrct = Math.round(allocated.toDouble / masterBal.toDouble * 100.0)
    val unallocated = masterBal - allocated
    val unallocPrct = Math.round(unallocated.toDouble / masterBal.toDouble * 100.0)

    val msg = "The master balance is " + masterBal + " KST, " + allocated + " KST (" +
      allocPrct + "%) of which are allocated, " + unallocated + " KST (" + unallocPrct +
      "%) are unallocated."

    src.sendMessage(
      Text.builder(msg)
        .color(TextColors.BLUE)
        .build()
    )

    CommandResult.success()
  }
}
