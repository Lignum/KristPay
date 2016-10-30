package me.lignum.kristpay

object Taxes {
  def apply(enabled: Boolean, withdrawMultiplier: Double, depositMultiplier: Double): Taxes =
    new Taxes(enabled, withdrawMultiplier, depositMultiplier)
}

class Taxes(var enabled: Boolean, var withdrawMultiplier: Double, var depositMultiplier: Double)
