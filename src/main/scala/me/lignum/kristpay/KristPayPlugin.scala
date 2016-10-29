package me.lignum.kristpay

import java.io.File
import java.net.URL

import com.google.inject.Inject
import me.lignum.kristpay.commands.{Balance, Pay, SetBalance}
import me.lignum.kristpay.economy.{KristCurrency, KristEconomy}
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.{GameInitializationEvent, GamePreInitializationEvent}
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.service.economy.EconomyService

@Plugin(
  id = "kristpay",
  name = "KristPay",
  version = "1.0.0",
  authors = Array("Lignum")
)
class KristPayPlugin {
  KristPayPlugin.instance = this

  val krist = new KristAPI(new URL("http://krist.ceriat.net"))
  var logger: Logger = _
  val currency = new KristCurrency

  var database: Database = _
  var masterWallet: MasterWallet = _

  var economyService: KristEconomy = _

  var failed = false

  @Inject
  def setLogger(lg: Logger) = logger = lg

  @Listener
  def onPreInit(event: GamePreInitializationEvent): Unit = {
    database = new Database(new File("kristpay.json"))
    economyService = new KristEconomy
    Sponge.getServiceManager.setProvider(this, classOf[EconomyService], economyService)
  }

  def fatalError(error: String, args: Any*): Unit = {
    logger.error("FATAL: " + error, args)
    failed = true
  }

  @Listener
  def onInit(event: GameInitializationEvent): Unit = {
    krist.getMOTD({
      case Some(motd) =>
        logger.info("Krist is up! The message of the day is \"{}\".", motd)

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

  def startPlugin(): Unit = {
    if (!failed) {
      Sponge.getCommandManager.register(this, Balance.spec, "balance", "bal")
      Sponge.getCommandManager.register(this, SetBalance.spec, "setbalance", "setbal")
      Sponge.getCommandManager.register(this, Pay.spec, "pay", "transfer")

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
