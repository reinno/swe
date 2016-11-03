package swe.service.Task

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.StatusCodes
import com.github.nscala_time.time.Imports._
import swe.SettingsActor
import swe.model.Activity
import swe.service.BaseService

import scala.concurrent.duration.{Duration => Duration4s, SECONDS}
import scala.language.postfixOps

object ActivityMaster {

  sealed trait Msg extends BaseService.Msg

  case class PostTask(entity: PostTaskEntity) extends Msg

  /* TODO fixme json4s Can't find constructor for entity in object PostTask
   * wired it is not happened in some environment */
  case class PostTaskEntity(name: String, version: Option[String] = None,
                            report_endpoint: Option[String] = None,
                            scheduleToStart: Option[Int] = None,
                            scheduleToClose: Option[Int] = None,
                            defaultTaskPriority: Int = 0,
                            heartbeatTimeout: Option[Int] = None,
                            startToCloseTimeout: Option[Int] = None,
                            input: Option[String] = None)

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

    case class Entity(status: String, details: Option[String] = None, output: Option[String] = None)

  }

  case class GetTask(runId: String) extends Msg

  case object GetTasks extends Msg {

    case class Response(instances: List[Activity.Instance])

  }

  case class PollTasks(entity: PollTasks.Entity) extends Msg

  object PollTasks {

    case class Entity(activityType: Activity.Type, num: Int = 1)

    case class Response(instances: List[Activity.InstanceInput])

  }

  def props(apiMaster: ActorRef): Props = Props(new ActivityMaster(apiMaster))

  def getRunId: String = java.util.UUID.randomUUID.toString
}

class ActivityMaster(apiMaster: ActorRef) extends BaseService with SettingsActor {

  import ActivityMaster._
  import context.dispatcher


  var taskWaitScheduled: List[Activity.Instance] = Nil
  var taskRunning: Map[String, Activity.Instance] = Map.empty
  var taskEnded: Map[String, Activity.Instance] = Map.empty

  context.system.scheduler.schedule(settings.activityCheckInterval, settings.activityCheckInterval, self, "check")

  log.info(s"${settings.defaultHeartBeatTimeout}")

  //noinspection ScalaStyle: CyclomaticComplexityChecker
  // scalastyle:off CyclomaticComplexityChecker
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
        taskRunning = taskRunning.updated(instance.runId,
          instance.copy(startTimeStamp = Some(DateTime.now),
            currentStatus = Activity.Status.Initialize.value)))
      sender ! PollTasks.Response(result._2.map(Activity.InstanceInput(_)))

    case msg: PostTaskStatus =>
      taskRunning.get(msg.runId) match {
        case Some(_) =>
          updateActivityStatus(msg.runId, msg.entity.status, msg.entity.details, msg.entity.output)
          sender ! StatusCodes.OK

        case None =>
          log.info(s"${msg.runId} activity not found")
          sender ! StatusCodes.NotFound
      }

    case msg: PostTaskHeartBeat =>
      taskRunning.get(msg.runId) match {
        case Some(_) =>
          updateActivityHeartBeat(msg.runId, msg.entity.details)
          sender ! StatusCodes.OK

        case None =>
          log.info(s"${msg.runId} activity not found")
          sender ! StatusCodes.NotFound
      }

    case msg: GetTask =>
      sender() ! getTask(msg.runId)

    case GetTasks =>
      sender ! GetTasks.Response((taskRunning.values.toList ++ taskEnded.values.toList ++ taskWaitScheduled)
        .sortBy(_.createTimeStamp).reverse)

    case "check" =>
      //log.info("check timeout")
      val now = DateTime.now
      taskRunning.values.foreach(instance => {
        if (isInstanceHeartbeatTimeout(instance, now)) {
          updateActivityStatus(instance.runId, Activity.Status.Timeout.value, Some("heartbeat timeout"))
        }
      })

    case x =>
      log.warning(s"receive unknown msg $x")
  }

  // scalastyle:on CyclomaticComplexityChecker

  private def getTask(runId: String): Option[Activity.Instance] = {
    (taskRunning ++ taskEnded).get(runId) match {
      case t: Some[Activity.Instance] =>
        t

      case None =>
        taskWaitScheduled.find(_.runId == runId)
    }
  }

  private def isInstanceHeartbeatTimeout(instance: Activity.Instance, now: DateTime): Boolean = {
    val timeout = instance.heartbeatTimeout + settings.defaultHeartBeatTimeoutCheckAppend
    def isTimeout(time: DateTime): Boolean = {
      (time to now).toDuration.getStandardSeconds > timeout.toSeconds
    }

    instance.lastHeartBeatTimeStamp match {
      case None =>
        instance.startTimeStamp match {
          case None =>
            log.warning("start timestamp not found")
            true
          case Some(time) =>
            isTimeout(time)
        }
      case Some(time) =>
        isTimeout(time)
    }
  }

  private def updateActivityHeartBeat(runId: String, details: Option[String] = None): Unit = {
    taskRunning.get(runId) match {
      case Some(activity) =>
        taskRunning = taskRunning.updated(runId, activity.copy(lastHeartBeatTimeStamp = Some(DateTime.now)))
        details match {
          case Some(s) if s.length != 0 =>
            updateActivityStatus(runId, Activity.Status.Running.value, details)

          case _ =>
        }

      case None =>
        log.warning(s"$runId activity not found")
    }
  }

  private def updateActivityStatus(runId: String,
                                   status: String,
                                   details: Option[String] = None,
                                   output: Option[String] = None): Unit = {
    taskRunning.get(runId) match {
      case Some(activity) =>
        if (isEndedStatus(status)) {
          taskEnded = taskEnded.updated(runId, activity.copy(currentStatus = status,
            output = output,
            closeStatus = Some(status),
            closeTimeStamp = Some(DateTime.now),
            history = activity.history :+ Activity.Event(DateTime.now, status, details)))
          taskRunning -= runId
        } else {
          taskRunning = taskRunning.updated(runId, activity.copy(currentStatus = status,
            history = activity.history :+ Activity.Event(DateTime.now, status, details)))
        }

      case None =>
        log.warning(s"$runId activity not found")
    }
  }

  private def isEndedStatus(status: String): Boolean = {
    Activity.Status.unapply(status) match {
      case s: Some[Activity.Status] if s.get.isEndedStatus =>
        true

      case _ =>
        false
    }
  }

  private def getActivityInstance(msg: PostTask): Activity.Instance = {
    val heartbeatTimeout = msg.entity.heartbeatTimeout
      .map(Duration4s(_, SECONDS)).getOrElse(settings.defaultHeartBeatTimeout)
    val startToCloseTimeout = msg.entity.heartbeatTimeout
      .map(Duration4s(_, SECONDS)).getOrElse(settings.defaultStartToEndTimeout)

    Activity.Instance(runId = getRunId,
      activityType = Activity.Type(msg.entity.name, msg.entity.version),
      createTimeStamp = DateTime.now,
      heartbeatTimeout = heartbeatTimeout,
      startToCloseTimeout = Some(startToCloseTimeout),
      currentStatus = Activity.Status.WaitScheduled.value,
      input = msg.entity.input,
      priority = msg.entity.defaultTaskPriority)
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
