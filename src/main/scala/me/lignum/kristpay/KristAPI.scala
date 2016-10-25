package me.lignum.kristpay

import java.net.URL

import org.json.JSONObject
import org.spongepowered.api.Sponge

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
