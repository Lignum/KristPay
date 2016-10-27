package me.lignum.kristpay.economy

import java.math.BigDecimal
import java.util

import me.lignum.kristpay.WalletAccount
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.service.economy.account.Account
import org.spongepowered.api.service.economy.transaction.{TransactionResult, TransferResult}
import org.spongepowered.api.text.Text

class KristAccount(val walletAccount: WalletAccount) extends Account {
  override def getDisplayName: Text = Text.of(walletAccount.owner)

  override def getBalances(contexts: util.Set[Context]): util.Map[Currency, BigDecimal] = null

  override def getBalance(currency: Currency, contexts: util.Set[Context]): BigDecimal = {
    BigDecimal.ZERO
  }

  override def transfer(to: Account, currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransferResult = null

  override def setBalance(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = null

  override def getDefaultBalance(currency: Currency): BigDecimal = BigDecimal.ZERO

  override def resetBalances(cause: Cause, contexts: util.Set[Context]): util.Map[Currency, TransactionResult] = null

  override def withdraw(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = null

  override def hasBalance(currency: Currency, contexts: util.Set[Context]): Boolean = false

  override def deposit(currency: Currency, amount: BigDecimal, cause: Cause, contexts: util.Set[Context]): TransactionResult = null

  override def resetBalance(currency: Currency, cause: Cause, contexts: util.Set[Context]): TransactionResult = null

  override def getIdentifier: String = walletAccount.owner

  override def getActiveContexts: util.Set[Context] = null
}
