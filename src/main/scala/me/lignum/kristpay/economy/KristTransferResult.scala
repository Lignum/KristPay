package me.lignum.kristpay.economy

import java.math.BigDecimal
import java.util

import me.lignum.kristpay.KristPay
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.service.economy.account.Account
import org.spongepowered.api.service.economy.transaction.{ResultType, TransactionType, TransferResult}

class KristTransferResult(
  val to: Account,            val from: Account,
  val c: Currency,            val amt: BigDecimal,
  val co: util.Set[Context],  val r: ResultType,
  val typ: TransactionType
) extends TransferResult {
  override def getAccountTo: Account = to

  override def getAccount: Account = from

  override def getType: TransactionType = typ

  override def getAmount: BigDecimal = amt

  override def getCurrency: Currency = KristPay.instance.currency

  override def getContexts: util.Set[Context] = co

  override def getResult: ResultType = r
}
