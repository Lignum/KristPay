package me.lignum.kristpay.economy

import java.util
import java.util.{Optional, UUID}

import me.lignum.kristpay.AccountAddresses
import org.spongepowered.api.service.context.ContextCalculator
import org.spongepowered.api.service.economy.{Currency, EconomyService}
import org.spongepowered.api.service.economy.account.{Account, UniqueAccount}

class KristEconomy(private val addresses: AccountAddresses) extends EconomyService {
  val kristCurrency = new KristCurrency

  override def getDefaultCurrency: Currency = kristCurrency

  override def getCurrencies: util.Set[Currency] = {
    val set = new util.HashSet[Currency]
    set.add(kristCurrency)
    set
  }

  override def hasAccount(identifier: String): Boolean = addresses.hasAccount(identifier)

  override def hasAccount(uuid: UUID): Boolean = hasAccount(uuid.toString)

  override def getOrCreateAccount(uuid: UUID): Optional[UniqueAccount] = Optional.empty()

  override def getOrCreateAccount(identifier: String): Optional[Account] = Optional.empty()

  override def registerContextCalculator(calculator: ContextCalculator[Account]): Unit = {

  }
}
