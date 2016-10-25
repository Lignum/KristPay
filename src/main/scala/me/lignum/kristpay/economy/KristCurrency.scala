package me.lignum.kristpay.economy

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.text.Text

class KristCurrency extends Currency {
  override def getDisplayName: Text = Text.of(getName)

  override def getPluralDisplayName: Text = getDisplayName

  override def getSymbol: Text = Text.of("KST")

  override def getDefaultFractionDigits: Int = 3

  override def isDefault: Boolean = true

  override def format(amount: BigDecimal, numFractionDigits: Int): Text = {
    val nf = NumberFormat.getInstance(Locale.ENGLISH)
    nf.setMaximumFractionDigits(numFractionDigits)
    Text.of(nf.format(amount))
  }

  override def getName: String = "Krist"

  override def getId: String = "kristpay:kristCurrency"
}
