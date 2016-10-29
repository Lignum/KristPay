package me.lignum.kristpay

// This looks like it could be a case class, and it could, but not
// in Sponge's strange delusional world.

object FloatingFunds {
  def apply(enabled: Boolean, interval: Int, threshold: Int) =
    new FloatingFunds(enabled, interval, threshold)
}

class FloatingFunds(var enabled: Boolean, var interval: Int, var threshold: Int) {}
