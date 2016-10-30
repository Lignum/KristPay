package me.lignum.kristpay.economy

import java.math.BigDecimal
import java.util

import me.lignum.kristpay.KristPay
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.service.economy.account.Account
import org.spongepowered.api.service.economy.transaction.{ResultType, TransactionResult, TransactionType}

class KristTransactionResult(
  val account: Account, val amt: BigDecimal, val ctx: util.Set[Context], res: ResultType, t: TransactionType
) extends TransactionResult {
  override def getType: TransactionType = t

  override def getAmount: BigDecimal = amt

  override def getCurrency: Currency = KristPay.get.currency

  override def getContexts: util.Set[Context] = ctx

  override def getResult: ResultType = res

  override def getAccount: Account = account
}
