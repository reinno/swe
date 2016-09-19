package swe.service

import akka.actor.{ActorLogging, Stash, Actor}

trait BaseService extends Actor with Stash with ActorLogging
