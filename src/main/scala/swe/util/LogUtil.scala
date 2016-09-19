package swe.util

import org.slf4j.{Logger, LoggerFactory}

object LogUtil {
  def getLogger[T](clazz: Class[T], context: String = "", name: String = ""): Logger = {
    var env = ""

    if ("" != context) {
      env += context
    }

    if ("" != name) {
      env += name
    }

    if (!env.isEmpty) {
      LoggerFactory.getLogger(clazz.getSimpleName + "@" + env)
    } else {
      LoggerFactory.getLogger(clazz.getSimpleName)
    }
  }
}
