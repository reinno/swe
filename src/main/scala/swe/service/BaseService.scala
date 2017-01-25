package swe.service

import akka.actor.{ActorLogging, Stash, Actor}

trait BaseService extends Actor with Stash with ActorLogging

object BaseService {
  trait Msg
  trait Notify
  trait Silent
}
