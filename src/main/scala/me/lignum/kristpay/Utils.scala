package me.lignum.kristpay

import java.util.concurrent.ThreadLocalRandom

object Utils {
  def generateKWPassword(length: Int): String = {
    val rand = ThreadLocalRandom.current()
    var pw = ""

    for (i <- 0 until length) {
      val j = rand.nextInt(33, 127)
      pw += j.toChar
    }

    pw
  }
}
