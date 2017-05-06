package eu.devtty.mboard

import eu.devtty.ipfs.api.IpfsApi
import org.scalajs.dom.window.location

import scala.scalajs.js.JSApp
import scala.scalajs.js

object AppLauncher extends JSApp {
  val port = if(location.port.nonEmpty) location.port else if(location.protocol.startsWith("https")) "443" else "80"

  val ipfs = new IpfsApi(location.hostname, port)

  def main(): Unit = {
    println("wiki-search")

    js.Dynamic.global.ipfs = ipfs.asInstanceOf[js.Any]
  }
}
