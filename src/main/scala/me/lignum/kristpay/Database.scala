package me.lignum.kristpay

import java.io.{File, PrintWriter}
import java.util.Scanner

import me.lignum.kristpay.economy.KristAccount
import org.json.{JSONArray, JSONException, JSONObject}

import scala.collection.mutable.ArrayBuffer

class Database(dbFile: File) {
  val floatingFunds = FloatingFunds(enabled = true, 3600, 15000)
  val taxes = Taxes(enabled = true, 0.08, 0.05)

  if (!dbFile.exists()) {
    try {
      dbFile.createNewFile()

      val d = new JSONObject()
      d.put("accounts", new JSONArray())

      val config = new JSONObject()
      // The master wallet's password.
      config.put("kwPassword", Utils.generateKWPassword(64))

      // Floating funds settings
      config.put("floatingEnabled", floatingFunds.enabled)

      // Any deposits greater than this threshold will be divided up
      // to be distributed over time.
      config.put("floatingThreshold", floatingFunds.threshold)

      // The time interval to distribute floating funds at.
      config.put("floatingInterval", floatingFunds.interval)

      // Taxes settings
      // If enabled, will tax players on deposits and withdraws,
      // which go towards the master wallet.
      config.put("taxesEnabled", taxes.enabled)

      // The value to multiply a value by to get the absolute amount of taxes.
      // valueInclTaxes = value - value * taxMultiplier
      config.put("taxesDepositMultiplier", taxes.depositMultiplier)
      config.put("taxesWithdrawMultiplier", taxes.withdrawMultiplier)

      d.put("config", config)

      val pw = new PrintWriter(dbFile)
      pw.write(d.toString())
      pw.close()
    } catch {
      case t: Throwable => KristPayPlugin.get.logger.error("Failed to create " + dbFile.getAbsolutePath, t)
    }
  }

  val accounts = ArrayBuffer[KristAccount]()
  var kwPassword: String = "0000"

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

      if (json.has("config")) {
        json.optJSONObject("config") match {
          case obj: JSONObject =>
            kwPassword = obj.optString("kwPassword", "0000")

            floatingFunds.enabled = obj.optBoolean("floatingEnabled", false)
            floatingFunds.interval = obj.optInt("floatingInterval", 3600)
            floatingFunds.threshold = obj.optInt("floatingThreshold", 15000)

            taxes.enabled = obj.optBoolean("taxesEnabled", true)
            taxes.depositMultiplier = obj.optDouble("taxesDepositMultiplier", 0.05)
            taxes.withdrawMultiplier = obj.optDouble("taxesWithdrawMultiplier", 0.05)

          case _ =>
        }
      }
    } catch {
      case e: JSONException =>
        KristPayPlugin.get.logger.error("Failed to parse {}: {}", dbFile.getName.asInstanceOf[Any], e.getMessage.asInstanceOf[Any])
      case t: Throwable =>
        KristPayPlugin.get.logger.info("Error while parsing kristpay config", t)
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

    val config = new JSONObject()
    config.put("kwPassword", kwPassword)

    config.put("floatingEnabled", floatingFunds.enabled)
    config.put("floatingThreshold", floatingFunds.threshold)
    config.put("floatingInterval", floatingFunds.interval)

    config.put("taxesEnabled", taxes.enabled)
    config.put("taxesDepositMultiplier", taxes.depositMultiplier)
    config.put("taxesWithdrawMultiplier", taxes.withdrawMultiplier)

    json.put("config", config)

    val pw = new PrintWriter(dbFile)
    pw.write(json.toString(4))
    pw.close()
  }

  load()
}
