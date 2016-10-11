package swe.service

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import akka.stream.Materializer
import swe.service.Task.{ActivityPoller, ActivityMaster}
import swe.util.ActorUtil


object ApiMaster {
  def props(httpClientFactory: HttpClientService.HttpClientFactory)(implicit mat: Materializer): Props = {
    Props(new ApiMaster(httpClientFactory))
  }
}

class ApiMaster(httpClientFactory: HttpClientService.HttpClientFactory)
               (implicit val mat: Materializer) extends BaseService {
  import context.dispatcher

  implicit val httpClientItf: HttpClientSender = httpClientFactory()
  val activityMaster = context.actorOf(ActivityMaster.props(self), "task-master")
  val activityPoller = context.actorOf(ActivityPoller.props(self), "task-poller")

  override def receive: Receive = {
    case msg: ActivityMaster.Msg =>
      forwardMsg(activityMaster, msg)

    case msg: ActivityPoller.Msg =>
      forwardMsg(activityPoller, msg)

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
