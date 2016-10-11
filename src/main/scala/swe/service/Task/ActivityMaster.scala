package swe.service.Task

import akka.actor.{ActorRef, Props}
import com.github.nscala_time.time.Imports.DateTime
import swe.model.Activity
import swe.service.BaseService

object ActivityMaster {
  sealed trait Msg extends BaseService.Msg

  case class PostTask(entity: PostTask.Entity) extends Msg
  object PostTask {
    case class Entity(name: String, version: Option[String] = None,
                      report_endpoint: Option[String] = None,
                      scheduleToStart: Option[Int] = None,
                      scheduleToClose: Option[Int] = None,
                      defaultTaskPriority: Int = 0,
                      heartbeatTimeout: Option[Int] = None,
                      startToCloseTimeout: Option[Int] = None,
                      input: Option[String] = None)
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

  case class PollTasks(entity: PollTasks.Entity)
  object PollTasks {
    case class Entity(activityType: Activity.Type, num: Int = 1)
    case class Response(instances: List[Activity.InstanceInput])
  }

  def props(apiMaster: ActorRef): Props = Props(new ActivityMaster(apiMaster))

  def getRunId: String = java.util.UUID.randomUUID.toString
}

class ActivityMaster(apiMaster: ActorRef) extends BaseService {
  import ActivityMaster._

  var taskWaitScheduled: List[Activity.Instance] = Nil
  var taskRunning: Map[String, Activity.Instance] = Map.empty
  var taskEnded: Map[String, Activity.Instance] = Map.empty

  override def receive: Receive = {
    case msg: PostTask =>
      val instance = getActivityInstance(msg)
      taskWaitScheduled = taskWaitScheduled :+ instance
      apiMaster ! ActivityPoller.NewTaskNotify(instance.activityType)
      sender ! instance.runId

    case msg: PollTasks =>
      val result = popWaitScheduledActivities(msg.entity, taskWaitScheduled)
      taskWaitScheduled = result._1
      result._2.foreach(instance =>
        taskRunning = taskRunning.updated(instance.runId, instance))
      sender ! PollTasks.Response(result._2.map(Activity.InstanceInput(_)))

    case msg: GetTask =>
      sender() ! getTask(msg.runId)

    case GetTasks =>
      sender ! GetTasks.Response((taskRunning ++ taskEnded).values.toList)

    case _ =>
  }

  private def getTask(runId: String): Option[Activity.Instance] = {
    (taskRunning ++ taskEnded).get(runId)
  }

  private def getActivityInstance(msg: PostTask): Activity.Instance = {
    Activity.Instance(runId = getRunId,
      activityType = Activity.Type(msg.entity.name, msg.entity.version),
      createTimeStamp = DateTime.now,
      currentStatus = Activity.Status.WaitScheduled.value)
  }

  private def popWaitScheduledActivities(param: PollTasks.Entity,
                                         activities: List[Activity.Instance])
  : (List[Activity.Instance], List[Activity.Instance]) = {
    activities.foldLeft[(List[Activity.Instance], List[Activity.Instance])]((Nil, Nil))(
      (result, activity) =>
        if (activityMatch(activity, param.activityType)) {
          (result._1, result._2 :+ activity)
        } else {
          (result._1 :+ activity, result._2)
        }
    )
  }

  private def activityMatch(activity: Activity.Instance, activityType: Activity.Type): Boolean = {
    if (activity.activityType.version.isEmpty || activityType.version.isEmpty) {
      activity.activityType.name == activityType.name
    } else {
      activity.activityType.name == activityType.name &&
        activity.activityType.version == activityType.version
    }
  }
}
