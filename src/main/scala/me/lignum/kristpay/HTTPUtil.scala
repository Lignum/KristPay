package me.lignum.kristpay

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}

import org.json.JSONObject

object HTTPUtil {
  def getConnection(url: URL): Option[HttpURLConnection] = try {
    val conn = url.openConnection()

    conn match {
      case httpConn: HttpURLConnection => Some(httpConn)
      case _ => None
    }
  } catch {
    case _: Throwable => None
  }

  def get(url: URL): Option[String] = {
    val mconn = getConnection(url)

    mconn match {
      case Some(conn) => try {
        conn.setRequestMethod("GET")

        val is = conn.getInputStream
        val br = new BufferedReader(new InputStreamReader(is))
        var response = ""

        var line = ""
        var loop = true

        while (loop) {
          line = br.readLine()

          if (line == null) {
            loop = false
          } else {
            response += line + "\n"
          }
        }

        br.close()
        Some(response)
      } catch {
        case _: Throwable => None
      }

      case None => None
    }
  }

  def post(url: URL, body: JSONObject): Option[String] = {
    val mconn = getConnection(url)

    mconn match {
      case Some(conn) => try {
        conn.setRequestMethod("POST")
        conn.setDoOutput(true)

        val jsonBody = body.toString(0)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Content-Length", String.valueOf(jsonBody.length))

        val os = conn.getOutputStream
        val bw = new BufferedWriter(new OutputStreamWriter(os))
        bw.write(jsonBody)
        bw.close()

        val is = conn.getInputStream
        val br = new BufferedReader(new InputStreamReader(is))
        var response = ""

        var line = ""
        var loop = true

        while (loop) {
          val line = br.readLine()

          if (line == null) {
            loop = false
          } else {
            response += line + "\n"
          }
        }

        br.close()
        Some(response)
      } catch {
        case _: Throwable => None
      }

      case None => None
    }
  }
}
