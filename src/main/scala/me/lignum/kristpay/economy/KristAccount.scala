package me.lignum.kristpay.economy

import java.math.BigDecimal
import java.util
import java.util.UUID

import me.lignum.kristpay.KristPayPlugin
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.service.economy.account.{Account, UniqueAccount}
import org.spongepowered.api.service.economy.transaction._
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

  override def transfer(to: Account, currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransferResult = to match {
    case target: KristAccount =>
      if (balance - amount.intValue() < 0) {
        return new KristTransferResult(to, this, currency, amount, contexts, ResultType.ACCOUNT_NO_FUNDS, TransactionTypes.TRANSFER)
      }

      balance -= amount.intValue()
      target.balance += amount.intValue()
      KristPayPlugin.get.database.save()
      new KristTransferResult(to, this, currency, amount, contexts, ResultType.SUCCESS, TransactionTypes.TRANSFER)

    case _ =>
      new KristTransferResult(to, this, currency, amount, contexts, ResultType.FAILED, TransactionTypes.TRANSFER)
  }

  override def setBalance(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = {
    if (amount.intValue() < 0) {
      // Balance should never be negative.
      return new KristTransactionResult(this, amount, contexts, ResultType.FAILED, TransactionTypes.WITHDRAW)
    }

    val delta = balance - amount.intValue()

    if (delta < 0) {
      // Increase in balance, check if the master wallet can fund this.
      val masterBal = KristPayPlugin.get.masterWallet.balance
      val used = KristPayPlugin.get.database.getTotalDistributedKrist

      val available = masterBal - used
      val increase = Math.abs(delta)

      if (increase > available) {
        // Not enough funds.
        new KristTransactionResult(this, BigDecimal.valueOf(0), contexts, ResultType.FAILED, TransactionTypes.DEPOSIT)
      } else {
        balance = amount.intValue()
        KristPayPlugin.get.database.save()
        new KristTransactionResult(this, amount, contexts, ResultType.SUCCESS, TransactionTypes.DEPOSIT)
      }
    } else if (delta > 0) {
      // Decrease in balance, no problem here.
      balance = amount.intValue()
      KristPayPlugin.get.database.save()
      new KristTransactionResult(this, amount, contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW)
    } else {
      // No change in balance.
      new KristTransactionResult(this, amount, contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW)
    }
  }

  override def getDefaultBalance(currency: Currency): BigDecimal = BigDecimal.ZERO

  override def resetBalances(cause: Cause, contexts: util.Set[Context]): util.Map[Currency, TransactionResult] = {
    val result = resetBalance(KristPayPlugin.instance.currency, cause, contexts)

    val map = new util.HashMap[Currency, TransactionResult]()
    map.put(KristPayPlugin.instance.currency, result)
    map
  }

  override def hasBalance(currency: Currency, contexts: util.Set[Context]): Boolean = true

  override def withdraw(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult =
    setBalance(currency, BigDecimal.valueOf(Math.max(0, balance - amount.intValue())), cause, contexts)

  override def deposit(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult =
    setBalance(currency, BigDecimal.valueOf(balance + amount.intValue()), cause, contexts)

  override def resetBalance(currency: Currency, cause: Cause, contexts: util.Set[Context]): TransactionResult = {
    val previous = balance
    balance = getDefaultBalance(currency).intValue()
    KristPayPlugin.get.database.save()

    new KristTransactionResult(
      this, BigDecimal.valueOf(previous), contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW
    )
  }

  override def getIdentifier = owner

  override def getActiveContexts: util.Set[Context] = null

  override def getUniqueId = UUID.fromString(owner)
}
