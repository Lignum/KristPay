package me.lignum.kristpay.economy

import java.math.BigDecimal
import java.util
import java.util.UUID

import me.lignum.kristpay.KristPayPlugin
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.service.economy.account.{Account, UniqueAccount}
import org.spongepowered.api.service.economy.transaction.{TransactionResult, TransferResult}
import org.spongepowered.api.text.Text

class KristAccount(val owner: String, var balance: Int = 0) extends UniqueAccount {
  override def getDisplayName: Text = Text.of(owner)

  override def getBalances(contexts: util.Set[Context]): util.Map[Currency, BigDecimal] = {
    val bals = new util.HashMap[Currency, BigDecimal]()
    bals.put(KristPayPlugin.instance.currency, getBalance(KristPayPlugin.instance.currency))
    bals
  }

  override def getBalances: util.Map[Currency, BigDecimal] = getBalances(null)

  override def getBalance(currency: Currency, contexts: util.Set[Context]): BigDecimal =
    BigDecimal.valueOf(balance.toLong)

  override def transfer(to: Account, currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransferResult = {
    null
  }

  override def setBalance(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = {
    null
  }

  override def getDefaultBalance(currency: Currency): BigDecimal = BigDecimal.ZERO

  override def resetBalances(cause: Cause, contexts: util.Set[Context]): util.Map[Currency, TransactionResult] = {
    null
  }

  override def withdraw(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = {
    null
  }

  override def hasBalance(currency: Currency, contexts: util.Set[Context]): Boolean = balance > 0

  override def deposit(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = null

  override def resetBalance(currency: Currency, cause: Cause, contexts: util.Set[Context]): TransactionResult = {
    balance = 0
    null
  }

  override def getIdentifier: String = owner

  override def getActiveContexts: util.Set[Context] = null

  override def getUniqueId: UUID = UUID.fromString(owner)
}
