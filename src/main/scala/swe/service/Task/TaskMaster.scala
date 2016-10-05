package swe.service.Task

import akka.actor.Props
import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import swe.model.Activity
import swe.service.{BaseService, HttpClientSender}
import com.github.nscala_time.time.Imports.DateTime

object TaskMaster {
  sealed trait Msg

  case class PostTask(entity: PostTask.Entity) extends Msg
  object PostTask {
    case class Entity(name: String, version: Option[String] = None,
                      report_endpoint: Option[String] = None,
                      scheduleToStart: Option[Int] = None,
                      scheduleToClose: Option[Int] = None,
                      defaultTaskPriority: Int = 0,
                      heartbeatTimeout: Option[Int] = None,
                      startToCloseTimeout: Option[Int] = None,
                      input: Option[String])
  }
  case class DeleteTask(taskId: String) extends Msg

  case class PostTaskHeartBeat(runId: String, entity: PostTaskHeartBeat.Entity) extends Msg
  object PostTaskHeartBeat {
    case class Entity(details: Option[String] = None)
  }

  case class PostTaskStatus(runId: String, entity: PostTaskStatus.Entity) extends Msg
  object PostTaskStatus {
    case class Entity(closeStatus: String, details: Option[String] = None, output: Option[String] = None)
  }

  case class GetTask(runId: String) extends Msg

  case object GetTasks extends Msg {
    case class Response(instances: List[Activity.Instance])
  }

  def props()(implicit httpClientItf: HttpClientSender, mat: Materializer): Props = Props(new TaskMaster())

  def getRunId: String = java.util.UUID.randomUUID.toString
}

class TaskMaster(implicit httpClientItf: HttpClientSender, implicit val mat: Materializer) extends BaseService {
  import TaskMaster._

  var taskWaitScheduled: Map[String, Activity.Instance] = Map.empty
  var taskRunning: Map[String, Activity.Instance] = Map.empty
  var taskEnded: Map[String, Activity.Instance] = Map.empty

  override def receive: Receive = {
    case msg: PostTask =>
      val instance = getActivityInstance(msg)
      taskWaitScheduled = taskWaitScheduled.updated(instance.runId, instance)
      sender ! StatusCodes.OK

    case msg: GetTask =>
      sender() ! getTask(msg.runId)

    case GetTasks =>
      sender ! GetTasks.Response((taskWaitScheduled ++ taskRunning ++ taskEnded).values.toList)

    case _ =>
  }

  private def getTask(runId: String): Option[Activity.Instance] = {
    (taskWaitScheduled ++ taskRunning ++ taskEnded).get(runId)
  }

  private def getActivityInstance(msg: PostTask): Activity.Instance = {
    Activity.Instance(runId = getRunId,
      activityType = Activity.Type(msg.entity.name, msg.entity.version),
      createTimeStamp = DateTime.now,
      currentStatus = Activity.Status.WaitScheduled.value)
  }
}
