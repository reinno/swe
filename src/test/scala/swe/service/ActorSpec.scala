package swe.service

import java.util.concurrent.TimeUnit

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.pattern.{ask, pipe}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

class MyActor(worker: ActorRef) extends Actor {
  import context.dispatcher
  override def receive: Receive = {
    case message@"initiate" =>
      val initiator = sender()
      println("received " + message)
      val f = ask(worker, "ask:" + message)(akka.util.Timeout(3, TimeUnit.SECONDS))
      f pipeTo initiator
    case msg =>
      println(msg)
  }
}

class ActorSpec extends TestKit(ActorSystem("MySpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with Matchers {
  val workerProbe = TestProbe()
  val initiatorProbe = TestProbe()

  val props = Props(new MyActor(workerProbe.ref))
  val subject = system.actorOf(props)

  subject.tell("initiate", initiatorProbe.ref)
  workerProbe.expectMsg("ask:initiate")
  workerProbe.sender.tell("responseFromWorker", workerProbe.ref)
  initiatorProbe.expectMsg("responseFromWorker")
}
