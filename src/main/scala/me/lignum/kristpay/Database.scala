package me.lignum.kristpay

import java.io.{File, PrintWriter}
import java.util.Scanner
import java.util.concurrent.ThreadLocalRandom

import org.json.{JSONArray, JSONObject}

import scala.collection.mutable.ArrayBuffer

class Database(dbFile: File) {
  if (!dbFile.exists()) {
    try {
      dbFile.createNewFile()

      val d = new JSONObject()
      d.put("accounts", new JSONArray())

      val config = new JSONObject()
      config.put("kwPassword", generatePassword(64))
      d.put("config", config)

      val pw = new PrintWriter(dbFile)
      pw.write(d.toString(4))
      pw.close()
    } catch {
      case t: Throwable => KristPayPlugin.get.logger.error("Failed to create " + dbFile.getAbsolutePath, t)
    }
  }

  private def generatePassword(length: Int): String = {
    val rand = ThreadLocalRandom.current()
    var pw = ""

    for (i <- 0 until length) {
      val j = rand.nextInt(33, 127)
      pw += j.toChar
    }

    pw
  }

  load()

  val accounts = ArrayBuffer[WalletAccount]()

  private def load(): Unit = {
    val scanner = new Scanner(dbFile)
    var contents = ""

    while (scanner.hasNextLine) {
      contents += scanner.nextLine()
    }

    scanner.close()

    val json = new JSONObject(contents)

    if (json.has("accounts")) {
      json.optJSONArray("accounts") match {
        case a: JSONArray =>
          for (i <- 0 until a.length()) {
            a.optJSONObject(i) match {
              case obj: JSONObject => accounts += WalletAccount(obj.optString("owner", ""), obj.optInt("balance", 0))
              case _ =>
            }
          }
        case _ =>
      }
    }
  }
}
