package me.lignum.kristpay

class Wallet(val password: String) {
  val privateKey: String = KristAPI.makePrivateKey(password)
  val address: String = KristAPI.makeAddressV2(privateKey)

  var balance: Int = 0

  def syncWithNode(callback: Boolean => Unit): Unit =
    KristPay.get.krist.getBalance(address, {
      case Some(bal) =>
        balance = bal
        callback(true)

      case None => callback(false)
    })

  def allocate(amount: Int): Int = Math.min(balance + amount, balance)

  def transfer(address: String, amount: Int, callback: Option[Boolean] => Unit) =
    KristPay.get.krist.transfer(privateKey, address, amount, callback)
}