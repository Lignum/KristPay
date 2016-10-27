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

  var masterAddress: String = _
  var masterPassword: String = _
  var masterPrivateKey: String = _

  @Inject
  def setLogger(lg: Logger) = logger = lg

  @Listener
  def onServerStart(event: GameStartedServerEvent): Unit = {
    database = new Database(new File("kristpay.json"))

    masterPassword = database.kwPassword
    masterPrivateKey = KristAPI.makePrivateKey(masterPassword)
    masterAddress = KristAPI.makeAddressV2(masterPrivateKey)

    logger.info("Using master address \"{}\"!", masterAddress)
  }
}

object KristPayPlugin {
  var instance: KristPayPlugin = _

  def get = instance
}
