package me.lignum.kristpay.economy

import java.math.BigDecimal
import java.util
import java.util.UUID

import me.lignum.kristpay.{KristPay, Utils, Wallet}
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.service.economy.account.{Account, UniqueAccount}
import org.spongepowered.api.service.economy.transaction._
import org.spongepowered.api.text.Text

class KristAccount(
  val owner: String, deposit: Option[Wallet] = None,
  var balance: Int = 0, val isUnique: Boolean = true
) extends UniqueAccount {
  var needsSave = false
  var depositLock = false

  val depositWallet = deposit match {
    case Some(wallet) =>
      needsSave = false
      wallet

    case None =>
      needsSave = true
      new Wallet(owner + Utils.generateKWPassword(64))
  }

  override def getDisplayName: Text = Text.of(owner)

  override def getBalances(contexts: util.Set[Context]): util.Map[Currency, BigDecimal] = {
    val bals = new util.HashMap[Currency, BigDecimal]()
    bals.put(KristPay.instance.currency, getBalance(KristPay.instance.currency))
    bals
  }

  override def getBalances: util.Map[Currency, BigDecimal] = getBalances(null)

  override def getBalance(currency: Currency, contexts: util.Set[Context]): BigDecimal =
    BigDecimal.valueOf(balance.toLong)

  override def transfer(to: Account, currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransferResult = to match {
    case target: KristAccount =>
      val baseTxLogString = owner + " - " + amount.intValue() + " -> " + target.owner + " (caused by " + cause.toString + ")"
      if (balance - amount.intValue() < 0) {
        KristPay.get.logTransaction(baseTxLogString + " => Failed (no funds)")
        return new KristTransferResult(to, this, currency, amount, contexts, ResultType.ACCOUNT_NO_FUNDS, TransactionTypes.TRANSFER)
      }

      if (amount.intValue() < 0) {
        KristPay.get.logTransaction(baseTxLogString + " => Failed (amount < 0)")
        return new KristTransferResult(to, this, currency, amount, contexts, ResultType.FAILED, TransactionTypes.TRANSFER)
      }

      balance -= amount.intValue()
      target.balance += amount.intValue()
      KristPay.get.database.save()
      KristPay.get.logTransaction(baseTxLogString + " => Success")
      new KristTransferResult(to, this, currency, amount, contexts, ResultType.SUCCESS, TransactionTypes.TRANSFER)

    case _ =>
      val baseTxLogString = owner + " - " + amount.intValue() + " -> ???"
      KristPay.get.logTransaction(baseTxLogString + " => Failed (target not a krist account)")
      new KristTransferResult(to, this, currency, amount, contexts, ResultType.FAILED, TransactionTypes.TRANSFER)
  }

  override def setBalance(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = {
    val baseTxLogString = "Change balance of " + owner + " to " + amount.intValue() + " (caused by " + cause.toString + ")"

    if (amount.intValue() < 0) {
      // Balance should never be negative.
      KristPay.get.logTransaction(baseTxLogString + " => Failed (negative balance)")
      return new KristTransactionResult(this, amount, contexts, ResultType.FAILED, TransactionTypes.WITHDRAW)
    }

    val delta = balance - amount.intValue()

    if (delta < 0) {
      // Increase in balance, check if the master wallet can fund this.
      val masterBal = KristPay.get.masterWallet.balance
      val used = KristPay.get.database.getTotalDistributedKrist

      val available = masterBal - used
      val increase = Math.abs(delta)

      if (increase > available) {
        // Not enough funds.
        KristPay.get.logTransaction(baseTxLogString + " => Failed (master wallet can't fund this)")
        new KristTransactionResult(this, BigDecimal.valueOf(0), contexts, ResultType.FAILED, TransactionTypes.DEPOSIT)
      } else {
        balance = amount.intValue()
        KristPay.get.database.save()
        KristPay.get.logTransaction(baseTxLogString + " => Success (deposit " + increase + " KST)")
        new KristTransactionResult(this, BigDecimal.valueOf(increase), contexts, ResultType.SUCCESS, TransactionTypes.DEPOSIT)
      }
    } else if (delta > 0) {
      // Decrease in balance, no problem here.
      val decrease = Math.abs(delta)
      balance = amount.intValue()
      KristPay.get.database.save()
      KristPay.get.logTransaction(baseTxLogString + " => Success (withdraw " + decrease + " KST)")
      new KristTransactionResult(this, BigDecimal.valueOf(Math.abs(delta)), contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW)
    } else {
      // No change in balance.
      KristPay.get.logTransaction(baseTxLogString + " => Success (no change)")
      new KristTransactionResult(this, BigDecimal.valueOf(0), contexts, ResultType.SUCCESS, TransactionTypes.WITHDRAW)
    }
  }

  override def getDefaultBalance(currency: Currency): BigDecimal = BigDecimal.ZERO

  override def resetBalances(cause: Cause, contexts: util.Set[Context]): util.Map[Currency, TransactionResult] = {
    val result = resetBalance(KristPay.instance.currency, cause, contexts)

    val map = new util.HashMap[Currency, TransactionResult]()
    map.put(KristPay.instance.currency, result)
    map
  }

  override def hasBalance(currency: Currency, contexts: util.Set[Context]): Boolean = true

  override def withdraw(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]) = {
    val amt = amount.intValue()
    val newBalance = balance - amt

    if (newBalance < 0) {
      new KristTransactionResult(this, amount, contexts, ResultType.ACCOUNT_NO_FUNDS, TransactionTypes.WITHDRAW)
    } else {
      setBalance(currency, BigDecimal.valueOf(newBalance), cause, contexts)
    }
  }

  override def deposit(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = {
    setBalance(currency, BigDecimal.valueOf(balance + amount.intValue()), cause, contexts)
  }

  override def resetBalance(currency: Currency, cause: Cause, contexts: util.Set[Context]): TransactionResult =
    setBalance(currency, getDefaultBalance(currency), cause, contexts)

  override def getIdentifier = owner

  override def getActiveContexts: util.Set[Context] = null

  override def getUniqueId = UUID.fromString(owner)
}
