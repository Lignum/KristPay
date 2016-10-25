package me.lignum.kristpay

case class Address(
  address: String,
  privateKey: Option[String] = None,
  balance: Int
)