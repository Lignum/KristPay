package me.lignum.kristpay

import java.io.{File, PrintWriter}
import java.util.Scanner

import me.lignum.kristpay.economy.KristAccount
import org.json.{JSONArray, JSONException, JSONObject}

import scala.collection.mutable.ArrayBuffer

class Database(dbFile: File) {
  if (!dbFile.exists()) {
    try {
      dbFile.createNewFile()

      val json = new JSONObject()
      json.put("accounts", new JSONArray())

      val pw = new PrintWriter(dbFile)
      pw.print(json.toString(4))
      pw.close()
    } catch {
      case e: Throwable => KristPay.get.logger.error("Could not create kristpay database.", e)
    }
  }

  val accounts = ArrayBuffer[KristAccount]()

  def getTotalDistributedKrist: Int = accounts.foldLeft(0) { (a, b) => a + b.balance }

  def load(): Unit = {
    val scanner = new Scanner(dbFile)
    var contents = ""

    while (scanner.hasNextLine) {
      contents += scanner.nextLine()
    }

    scanner.close()

    try {
      val json = new JSONObject(contents)

      if (json.has("accounts")) {
        json.optJSONArray("accounts") match {
          case a: JSONArray =>
            for (i <- 0 until a.length()) {
              a.optJSONObject(i) match {
                case obj: JSONObject =>
                  val depositWallet = obj.optString("depositPassword") match {
                    case pw: String => Some(new Wallet(pw))
                    case _ => None
                  }

                  accounts += new KristAccount(
                    obj.optString("owner", ""), depositWallet, obj.optInt("balance", 0)
                  )
                case _ =>
              }
            }
          case _ =>
        }
      }
    } catch {
      case e: JSONException =>
        KristPay.get.logger.error("Failed to parse {}: {}", dbFile.getName.asInstanceOf[Any], e.getMessage.asInstanceOf[Any])
      case t: Throwable =>
        KristPay.get.logger.info("Error while parsing kristpay config", t)
    }

    if (accounts.foldLeft(false) { (a, b) => a || b.needsSave }) {
      save()
    }
  }

  def save(): Unit = {
    val json = new JSONObject()
    val accs = new JSONArray()

    accounts.foreach(acc => {
      val obj = new JSONObject()
      obj.put("owner", acc.owner)
      obj.put("balance", acc.balance)
      obj.put("depositPassword", acc.depositWallet.password)
      accs.put(obj)
    })

    json.put("accounts", accs)

    val pw = new PrintWriter(dbFile)
    pw.write(json.toString(4))
    pw.close()
  }

  load()
}
