package me.lignum.kristpay

import java.io.File
import java.net.URL

import com.google.inject.Inject
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

  var database: Database = _

  @Inject
  def setLogger(lg: Logger) = logger = lg

  @Listener
  def onServerStart(event: GameStartedServerEvent): Unit = {
    database = new Database(new File("kristpay.json"))
  }
}

object KristPayPlugin {
  var instance: KristPayPlugin = _

  def get = instance
}
