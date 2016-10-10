package swe.service.Task

import akka.stream.Materializer
import swe.model.Activity
import swe.service.{BaseService, HttpClientSender}

object TaskPoller {
  sealed trait Msg
  case class NewTaskNotify(activityType: Activity.Type) extends Msg
}

class TaskPoller(implicit httpClientItf: HttpClientSender, implicit val mat: Materializer) extends BaseService {
  import TaskPoller._

  //start timer

  override def receive: Receive = {
    case msg: NewTaskNotify =>
      // find task service
      // poll task
      context.parent ! TaskMaster.PollTasks(TaskMaster.PollTasks.Entity(msg.activityType, 1))

    case _ =>
  }
}
