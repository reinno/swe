package swe.util

import java.util.concurrent.TimeUnit

object Constants {
  val REST_VERSION = "v1"

  val SERVER_PORT = 8001
  val SERVER_HOST = "0.0.0.0"

  val FUTURE_TIMEOUT = akka.util.Timeout(15, TimeUnit.SECONDS)
}
