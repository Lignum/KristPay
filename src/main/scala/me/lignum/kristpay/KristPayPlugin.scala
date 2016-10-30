package me.lignum.kristpay

import java.io.{File, FileOutputStream, PrintWriter}
import java.net.URL
import java.text.SimpleDateFormat
import java.util.{Date, UUID}
import java.util.concurrent.TimeUnit

import com.google.inject.Inject
import me.lignum.kristpay.commands._
import me.lignum.kristpay.economy.{KristAccount, KristCurrency, KristEconomy}
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.cause.{Cause, NamedCause}
import org.spongepowered.api.event.game.state.{GameInitializationEvent, GamePreInitializationEvent, GameStoppingServerEvent}
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.service.economy.EconomyService
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

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
  var nextPayoutTime: Long = System.currentTimeMillis()

  var failed = false

  val txLogFile = new File("kristpay.log")
  var txOut: PrintWriter = _

  @Inject
  def setLogger(lg: Logger) = logger = lg

  @Listener
  def onPreInit(event: GamePreInitializationEvent): Unit = {
    database = new Database(new File("kristpay.json"))
    economyService = new KristEconomy

    txOut = new PrintWriter(new FileOutputStream(txLogFile, true))

    Sponge.getServiceManager.setProvider(this, classOf[EconomyService], economyService)
  }

  val txLogDateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss")

  def logTransaction(msg: String): Unit = {
    val now = new Date()
    txOut.println(txLogDateFormat.format(now) + " => " + msg)
    txOut.flush()
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

  private def makeDeposit(acc: KristAccount, amount: Int, message: Int => String): Unit =
    acc.depositWallet.transfer(masterWallet.address, amount, {
      case Some(ok) => if (ok) {
        masterWallet.syncWithNode(okk => if (okk) {
          acc.deposit(
            currency, java.math.BigDecimal.valueOf(amount),
            Cause.of(NamedCause.source(this)), null
          )

          if (acc.isUnique) {
            // This is a player, let's send them a message.
            Sponge.getServer.getPlayer(UUID.fromString(acc.owner)).ifPresent(ply => {
              if (ply.isOnline) {
                ply.sendMessage(
                  Text.builder(message(amount))
                    .color(TextColors.GREEN)
                    .build()
                )
              }
            })
          }
        })
      }

      case None =>
    })

  def startFloatingDepositSchedule(): Unit = {
    Sponge.getScheduler.createTaskBuilder()
      .interval(database.floatingFunds.interval, TimeUnit.SECONDS)
      .execute(_ => {
        database.accounts.foreach(acc => {
          acc.depositWallet.syncWithNode(ok => if (ok) {
            if (acc.depositWallet.balance > 0) {
              val depositAmount = Math.min(acc.depositWallet.balance, database.floatingFunds.threshold)
              makeDeposit(
                acc, depositAmount,
                amt => {
                  val newDepositBalance = acc.depositWallet.balance - depositAmount
                  val nextDepositAmount =
                    Math.min(newDepositBalance, database.floatingFunds.threshold)

                  val have = if (amt > 1) "have" else "has"
                  val base = amt + " KST " + have + " been deposited to your account."

                  if (nextDepositAmount > 0) {
                    base +
                      " You will receive " + nextDepositAmount + " KST (out of " + newDepositBalance + " KST)" +
                      " in " + database.floatingFunds.interval + "s."
                  } else {
                    base
                  }
                }
              )
            }
          })
        })

        nextPayoutTime = System.currentTimeMillis() + (database.floatingFunds.interval * 1000L)
      })
      .submit(this)
  }

  def startDepositSchedule(): Unit = {
    Sponge.getScheduler.createTaskBuilder()
      .interval(5, TimeUnit.SECONDS)
      .execute(_ => database.accounts.foreach(acc => {
        acc.depositWallet.syncWithNode(ok => if (ok) {
          if (acc.depositWallet.balance > 0) {
            makeDeposit(
              acc, acc.depositWallet.balance,
              amt => amt + " KST " + (if (amt > 1) "have" else "has") + " been deposited to your account."
            )
          }
        })
      }))
      .submit(this)
  }

  @Listener
  def onServerStopping(event: GameStoppingServerEvent): Unit = {
    database.save()
    txOut.close()
  }

  def startPlugin(): Unit = {
    if (!failed) {
      Sponge.getCommandManager.register(this, Balance.spec, "balance", "bal")
      Sponge.getCommandManager.register(this, SetBalance.spec, "setbalance", "setbal")
      Sponge.getCommandManager.register(this, Pay.spec, "pay", "transfer")
      Sponge.getCommandManager.register(this, Withdraw.spec, "withdraw")
      Sponge.getCommandManager.register(this, Deposit.spec, "deposit")
      Sponge.getCommandManager.register(this, Payout.spec, "payout", "nextpayout")

      logger.info("Using master address \"{}\"!", masterWallet.address)
      masterWallet.startSyncSchedule()

      if (database.floatingFunds.enabled) {
        startFloatingDepositSchedule()
      } else {
        startDepositSchedule()
      }
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
