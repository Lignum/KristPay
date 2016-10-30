package me.lignum.kristpay

import java.util.concurrent.TimeUnit

import org.spongepowered.api.Sponge

class MasterWallet(password: String) extends Wallet(password) {
  def isExhausted = KristPay.get.database.getTotalDistributedKrist >= balance

  def isAlmostExhausted(threshold: Double) =
    (KristPay.get.database.getTotalDistributedKrist.toDouble / balance.toDouble) >= threshold

  def startSyncSchedule(): Unit =
    Sponge.getScheduler.createTaskBuilder
      .async()
      .interval(5, TimeUnit.SECONDS)
      .execute(_ => syncWithNode(ok => {
        if (!ok) {
          KristPay.get.logger.warn("Could not sync master wallet with Krist node!")
        } else {
          if (isExhausted) {
            KristPay.get.logger.warn("!!! The master wallet is exhausted !!!")
          } else if (isAlmostExhausted(0.95)) {
            KristPay.get.logger.warn(
              "!! The master wallet is almost exhausted. " +
              KristPay.get.database.getTotalDistributedKrist + " KST / " + balance + " KST used!"
            )
          }
        }
      }))
      .submit(KristPay.get)
}