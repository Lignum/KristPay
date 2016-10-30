package me.lignum.kristpay

import java.io.File

import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader

class Configuration(configFile: File) {
  private val loader = HoconConfigurationLoader.builder()
    .setFile(configFile)
    .build()

  private var rootNode: ConfigurationNode = _

  val floatingFunds = FloatingFunds(enabled = true, 3600, 15000)
  val taxes = Taxes(enabled = true, 0.02, 0.02)

  var kwPassword: String = _

  private def load(): Unit = {
    if (loader.canLoad) {
      rootNode = loader.load()
    } else {
      KristPay.get.logger.error("Can't load config file!")
    }

    if (rootNode.getNode("kwPassword").isVirtual) {
      // The master wallet's password.
      rootNode.getNode("kwPassword").setValue(Utils.generateKWPassword(64))

      // Floating funds settings
      rootNode.getNode("floating", "enabled").setValue(floatingFunds.enabled)

      // Any deposits greater than this threshold will be divided up
      // to be distributed over time.
      rootNode.getNode("floating", "threshold").setValue(floatingFunds.threshold)

      // The time interval to distribute floating funds at.
      rootNode.getNode("floating", "interval").setValue(floatingFunds.interval)

      // Taxes settings
      // If enabled, will tax players on deposits and withdraws,
      // which go towards the master wallet.
      rootNode.getNode("taxes", "enabled").setValue(taxes.enabled)

      // The value to multiply a value by to get the absolute amount of taxes.
      // valueInclTaxes = value - value * taxMultiplier
      rootNode.getNode("taxes", "depositMultiplier").setValue(taxes.depositMultiplier)
      rootNode.getNode("taxes", "withdrawMultiplier").setValue(taxes.withdrawMultiplier)
    }

    if (loader.canSave) {
      loader.save(rootNode)
    } else {
      KristPay.get.logger.error("Can't save default config file!")
    }

    kwPassword = rootNode.getNode("kwPassword").getString("0000")

    floatingFunds.enabled = rootNode.getNode("floating", "enabled").getBoolean(floatingFunds.enabled)
    floatingFunds.threshold = rootNode.getNode("floating", "threshold").getInt(floatingFunds.threshold)
    floatingFunds.interval = rootNode.getNode("floating", "interval").getInt(floatingFunds.interval)

    taxes.enabled = rootNode.getNode("taxes", "enabled").getBoolean(taxes.enabled)
    taxes.depositMultiplier = rootNode.getNode("taxes", "depositMultiplier").getDouble(taxes.depositMultiplier)
    taxes.withdrawMultiplier = rootNode.getNode("taxes", "withdrawMultiplier").getDouble(taxes.withdrawMultiplier)
  }

  load()
}
