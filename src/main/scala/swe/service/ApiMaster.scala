package swe.service

import akka.actor.Props
import akka.pattern.pipe
import akka.stream.Materializer
import swe.service.Task.TaskMaster
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
  val taskMaster = context.actorOf(TaskMaster.props(self), "task-master")

  override def receive: Receive = {
    case msg: TaskMaster.Msg =>
      val asker = sender()
      ActorUtil.askActor(taskMaster, msg) pipeTo asker

    case msg =>
      log.warning(s"unknown msg: $msg")
      sender() ! "unknown msg"
  }
}
