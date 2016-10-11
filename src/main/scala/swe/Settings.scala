package swe

import akka.actor.{Actor, ExtendedActorSystem, Extension, ExtensionKey}

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {
  val defaultHeartBeatTimeout = system.settings.config.getInt("swe.activity.defaultHeartBeatTimeout")
  val defaultStartToEndTimeout = system.settings.config.getInt("swe.activity.defaultStartToEndTimeout")

  val bindAddr = system.settings.config.getString("swe.server.addr")
  val bindPort = system.settings.config.getInt("swe.server.port")
}

trait SettingsActor {
  this: Actor =>

  val settings: Settings =
    Settings(context.system)
}