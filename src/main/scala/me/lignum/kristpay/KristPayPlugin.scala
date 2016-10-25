package me.lignum.kristpay

import java.net.URL

import org.json.JSONObject
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

  @Listener
  def onServerStart(event: GameStartedServerEvent): Unit = {
    krist.submitGet("/motd", {
      case Some(msg) =>
        val json = new JSONObject(msg)

        if (json.getBoolean("ok")) {
          println("Krist MOTD is: " + json.getString("motd"))
        } else {
          println("Krist MOTD is not ok :(")
        }
      case None => println("Failed to get Krist MOTD!!")
    })
  }
}

object KristPayPlugin {
  var instance: KristPayPlugin = _
}
