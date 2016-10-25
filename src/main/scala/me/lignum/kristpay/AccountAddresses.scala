package me.lignum.kristpay

import scala.collection.mutable

class AccountAddresses {
  private val accounts = mutable.HashMap[String, Address]()

  def getAddressFor(identifier: String): Option[Address] = accounts.get(identifier)

  def hasAccount(identifier: String) = getAddressFor(identifier) match {
    case Some(_) => true
    case None => false
  }
}
