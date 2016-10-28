package me.lignum.kristpay

import java.util.concurrent.TimeUnit

import org.spongepowered.api.Sponge

class MasterWallet(val password: String) {
  val privateKey: String = KristAPI.makePrivateKey(password)
  val address: String = KristAPI.makeAddressV2(privateKey)

  private var balance: Int = 0

  def startSyncSchedule(): Unit =
    Sponge.getScheduler.createTaskBuilder
      .async()
      .interval(5, TimeUnit.SECONDS)
      .execute(_ => syncWithNode())
      .submit(KristPayPlugin.get)

  def syncWithNode(): Unit = {
    KristPayPlugin.get.krist.getBalance(address, {
      case Some(bal) => balance = bal
      case None => KristPayPlugin.get.logger.error("Failed to sync balance with the Krist node!!")
    })
  }

  def allocate(amount: Int): Int = Math.min(balance + amount, balance)
}
