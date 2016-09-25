package swe.service

import akka.actor.Props
import akka.stream.Materializer
import swe.service.Task.TaskMaster


object ApiMaster {
  def props(httpClientFactory: HttpClientService.HttpClientFactory)(implicit mat: Materializer): Props = {
    Props(new ApiMaster(httpClientFactory))
  }
}

class ApiMaster(httpClientFactory: HttpClientService.HttpClientFactory)
               (implicit val mat: Materializer) extends BaseService {
  implicit val httpClientItf: HttpClientSender = httpClientFactory()
  val taskMaster = context.actorOf(TaskMaster.props(), "task-master")

  override def receive: Receive = {
    case msg: TaskMaster.Msg =>
      taskMaster ! msg

    case msg =>
      log.warning(s"unknown msg: $msg")
      sender() ! "unknown msg"
  }
}
