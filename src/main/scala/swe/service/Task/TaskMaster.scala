package swe.service.Task

import akka.actor.Props
import akka.stream.Materializer
import swe.interface.TaskInterface
import swe.service.{HttpClientSender, BaseService}

object TaskMaster {
  sealed trait Msg
  case class PostTask(entity: TaskInterface.PostTask) extends Msg
  case class DeleteTask(taskId: String) extends Msg
  case class PostTaskHeartBeat(taskId: String, entity: TaskInterface.PostTaskHeartBeat) extends Msg
  case class PostTaskStatus(taskId: String, entity: TaskInterface.PostTaskStatus) extends Msg

  def props()(implicit httpClientItf: HttpClientSender, mat: Materializer): Props = Props(new TaskMaster())
}

class TaskMaster(implicit httpClientItf: HttpClientSender, implicit val mat: Materializer) extends BaseService {
  override def receive: Receive = {
    case _ =>
  }
}
