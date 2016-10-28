package me.lignum.kristpay

import java.io.File
import java.net.URL

import com.google.inject.Inject
import me.lignum.kristpay.economy.KristCurrency
import org.slf4j.Logger
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GameStartedServerEvent
import org.spongepowered.api.plugin.Plugin

@Plugin(
  id = "kristpay",
  name = "KristPay",
  version = "1.0.0",
  authors = Array("Lignum")
)
class KristPayPlugin {
  KristPayPlugin.instance = this

  val krist = new KristAPI(new URL("https://krist.ceriat.net"))
  var logger: Logger = _
  val currency = new KristCurrency

  var database: Database = _
  var masterWallet: MasterWallet = _

  var failed = false

  @Inject
  def setLogger(lg: Logger) = logger = lg

  @Listener
  def onServerStart(event: GameStartedServerEvent): Unit = {
    krist.getMOTD({
      case Some(motd) =>
        logger.info("Krist is up! The message of the day is \"{}\".", motd)

        database = new Database(new File("kristpay.json"))
        masterWallet = new MasterWallet(database.kwPassword)

        krist.doesAddressExist(masterWallet.address, {
          case Some(exists) =>
            if (!exists)
              fatalError(KristPayPlugin.ADDRESS_DOESNT_EXIST_MSG, masterWallet.address)
            startPlugin()

          case None =>
            fatalError(KristPayPlugin.ADDRESS_DOESNT_EXIST_MSG, masterWallet.address)
            startPlugin()
        })

      case None => fatalError("Krist is down!!")
    })
  }

  def fatalError(error: String, args: Any*): Unit = {
    logger.error("FATAL: " + error, args)
    failed = true
  }

  def startPlugin(): Unit = {
    if (!failed) {
      logger.info("Using master address \"{}\"!", masterWallet.address)
      masterWallet.startSyncSchedule()
    } else {
      logger.error("Can't start KristPay due to a fatal error.")
    }
  }
}

object KristPayPlugin {
  val ADDRESS_DOESNT_EXIST_MSG = "The address {} doesn't exist."

  var instance: KristPayPlugin = _

  def get = instance
}
