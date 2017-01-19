package swe.service

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import akka.stream.Materializer
import swe.service.Task.{ActivityPassivePoller, ActivityMaster, ActivityPoller}
import swe.util.ActorUtil


object ApiMaster {
  def props()(implicit mat: Materializer, httpClientFactory: HttpClientService.HttpClientFactory): Props = {
    Props(new ApiMaster())
  }
}

class ApiMaster(implicit val mat: Materializer, val httpClientFactory: HttpClientService.HttpClientFactory) extends BaseService {
  import context.dispatcher

  val activityMaster = context.actorOf(ActivityMaster.props(self), "task-master")
  val activityPoller = context.actorOf(ActivityPoller.props(self), "task-poller")
  val activityPollerPassive = context.actorOf(ActivityPassivePoller.props(self), "task-poller-passive")

  override def receive: Receive = {
    case msg: ActivityMaster.Msg =>
      log.debug(s"send $msg to activity master")
      forwardMsg(activityMaster, msg)

    case msg: ActivityPoller.Msg =>
      log.debug(s"send $msg to activity poller")
      forwardMsg(activityPoller, msg)

    case msg: ActivityPassivePoller.Msg =>
      log.debug(s"send $msg to activity poller-passive")
      forwardMsg(activityPollerPassive, msg)

    case msg =>
      log.warning(s"unknown msg: $msg")
      sender() ! "unknown msg"
  }

  private def forwardMsg(target: ActorRef, msg: BaseService.Msg): Unit = {
    msg match {
      case m: BaseService.Notify =>
        target ! msg

      case _ =>
        val requester = sender()
        ActorUtil.askActor(target, msg) pipeTo requester
    }
  }
}
