package me.lignum.kristpay

import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest

import org.json.JSONObject
import org.spongepowered.api.Sponge

import scala.collection.mutable.ArrayBuffer

object KristAPI {
  def sha256(toHash: String): String = {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(toHash.getBytes("UTF-8"))
    val digest = md.digest()
    String.format("%064x", new BigInteger(1, digest))
  }

  def numToChar(inp: Int): Char = {
    for (i <- 6 to 251 by 7) {
      if (inp <= i)
        if (inp <= 69)
          return ('0'.toInt + (i - 6) / 7).toChar
        else
          return ('a'.toInt + (i - 76) / 7).toChar
    }

    'e'
  }

  def makePrivateKey(password: String): String = sha256("KRISTWALLET" + password) + "-000"

  def makeAddressV1(pkey: String): String = sha256(pkey).substring(0, 10)

  def makeAddressV2(pkey: String): String = {
    val chars = ArrayBuffer("", "", "", "", "", "", "", "", "")
    var address = "k"
    var hash = sha256(sha256(pkey))

    for (i <- 0 to 8) {
      chars(i) = hash.substring(0, 2)
      hash = sha256(sha256(hash))
    }

    var i = 0

    while (i <= 8) {
      val index = Integer.parseInt(hash.substring(2 * i, 2 + 2 * i), 16) % 9

      if (chars(index).equals("")) {
        hash = sha256(hash)
      } else {
        address += numToChar(Integer.parseInt(chars(index), 16))
        chars(index) = ""
        i = i + 1
      }
    }

    address
  }
}

class KristAPI(val node: URL) {
  def submitGet(route: String, callback: Option[String] => Unit) = {
    val url = new URL(node, route)

    val scheduler = Sponge.getScheduler

    scheduler.createTaskBuilder()
      .async()
      .execute(_ => {
        val result = HTTPUtil.get(url)

        scheduler.createTaskBuilder()
          .execute(_ => callback(result))
          .submit(KristPayPlugin.instance)
      })
      .submit(KristPayPlugin.instance)
  }

  def submitPost(route: String, body: JSONObject, callback: Option[String] => Unit): Unit = {
    val url = new URL(node, route)

    val scheduler = Sponge.getScheduler

    scheduler.createTaskBuilder()
      .async()
      .execute(_ => {
        val result = HTTPUtil.post(url, body)

        scheduler.createTaskBuilder()
          .execute(_ => callback(result))
          .submit(KristPayPlugin.instance)
      })
      .submit(KristPayPlugin.instance)
  }
}
