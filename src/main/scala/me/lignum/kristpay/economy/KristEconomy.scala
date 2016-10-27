package me.lignum.kristpay.economy

import java.util
import java.util.{Optional, UUID}

import me.lignum.kristpay.{WalletAccount, KristPayPlugin}
import org.spongepowered.api.service.context.ContextCalculator
import org.spongepowered.api.service.economy.account.{Account, UniqueAccount}
import org.spongepowered.api.service.economy.{Currency, EconomyService}

class KristEconomy extends EconomyService {
  val kristCurrency = new KristCurrency

  override def getDefaultCurrency: Currency = kristCurrency

  override def getCurrencies: util.Set[Currency] = {
    val set = new util.HashSet[Currency]
    set.add(kristCurrency)
    set
  }

  override def hasAccount(identifier: String): Boolean =
    KristPayPlugin.get.database.accounts.exists(_.owner == identifier)

  override def hasAccount(uuid: UUID): Boolean = hasAccount(uuid.toString)

  override def getOrCreateAccount(uuid: UUID): Optional[UniqueAccount] =
    Optional.empty()

  override def getOrCreateAccount(identifier: String): Optional[Account] = {
    val wacc = KristPayPlugin.get.database.accounts.find(_.owner == identifier)

    wacc match {
      case Some(w) => Optional.of(new KristAccount(w))
      case None =>
        val nacc = WalletAccount(identifier, 0)
        KristPayPlugin.get.database.accounts += nacc
        Optional.of(new KristAccount(nacc))
    }
  }

  override def registerContextCalculator(calculator: ContextCalculator[Account]): Unit = {

  }
}
