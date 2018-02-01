package me.lignum.kristpay.economy

import java.util
import java.util.{Optional, UUID}

import me.lignum.kristpay.KristPay
import org.spongepowered.api.event.cause.{Cause, EventContext}
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
    KristPay.get.database.accounts.exists(_.owner == identifier)

  override def hasAccount(uuid: UUID): Boolean = hasAccount(uuid.toString)

  override def getOrCreateAccount(uuid: UUID): Optional[UniqueAccount] = {
    val opt = getOrCreateAccount(uuid.toString)
    
    if (opt.isPresent) {
      val acc = opt.get()

      acc match {
        case account: UniqueAccount => Optional.of(account)
        case _ => Optional.empty()
      }
    } else {
      Optional.empty()
    }
  }

  override def getOrCreateAccount(identifier: String): Optional[Account] =
    KristPay.get.database.accounts.find(_.owner.equalsIgnoreCase(identifier)) match {
      case Some(acc) => Optional.of(acc)
      case None =>
        val nacc = new KristAccount(identifier)
        nacc.setBalance(
          getDefaultCurrency,
          nacc.getDefaultBalance(getDefaultCurrency),
          Cause.of(EventContext.empty(), this),
          null
        )

        KristPay.get.database.accounts += nacc
        KristPay.get.database.save()
        Optional.of(nacc)
    }

  override def registerContextCalculator(calculator: ContextCalculator[Account]): Unit = {}
}
