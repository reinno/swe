package swe

import scala.concurrent.duration._
import akka.actor.{Actor, ExtendedActorSystem, Extension, ExtensionKey}

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {
  val defaultHeartBeatTimeout = Duration(system.settings.config.getDuration("swe.activity.defaultHeartBeatTimeout", SECONDS), SECONDS)
  val defaultStartToEndTimeout = Duration(system.settings.config.getDuration("swe.activity.defaultStartToEndTimeout", SECONDS), SECONDS)
  val activityCheckInterval = Duration(system.settings.config.getDuration("swe.activity.checkInterval", SECONDS), SECONDS)

  val bindAddr = system.settings.config.getString("swe.server.addr")
  val bindPort = system.settings.config.getInt("swe.server.port")
}

trait SettingsActor {
  this: Actor =>

  val settings: Settings =
    Settings(context.system)
}