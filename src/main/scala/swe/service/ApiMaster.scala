package swe.service

import akka.stream.Materializer
import swe.service.Task.TaskMaster

class ApiMaster(implicit httpClientItf: HttpClientSender, implicit val mat: Materializer) extends BaseService {
  val taskMaster = context.actorOf(TaskMaster.props(), "task-master")

  override def receive: Receive = {
    case msg: TaskMaster.Msg =>
      taskMaster ! msg

    case _ =>
  }
}
