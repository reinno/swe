package swe.model

import scala.concurrent.duration.Duration
import com.github.nscala_time.time.Imports.DateTime

object Activity {

  case class Type(name: String, version: Option[String] = None)

  case class Configuration(description: String,
                           defaultTaskList: String,
                           defaultTaskScheduleToStart: Option[Duration] = Some(Duration.Inf),
                           defaultTaskScheduleToClose: Option[Duration] = Some(Duration.Inf),
                           defaultTaskPriority: Int = 0,
                           defaultTaskHeartbeatTimeout: Option[Duration] = Some(Duration.Inf),
                           defaultTaskStartToCloseTimeout: Option[Duration] = Some(Duration.Inf))


  object InstanceInput {
    def apply(instance: Instance): InstanceInput ={
      InstanceInput(instance.activityId, instance.runId,
        instance.activityType, instance.workflowExecution, instance.startedEventId, instance.input)
    }
  }
  case class InstanceInput(activityId: Option[String],
                           runId: String,
                           activityType: Activity.Type,
                           workflowExecution: Option[Workflow.Instance] = None,
                           startedEventId: Option[String],
                           input: Option[String] = None)


  case class InstanceInfo(execution: Option[Workflow.Instance] = None,
                          activityType: Activity.Type,
                          startTimeStamp: DateTime,
                          lastHeartBeatTimeStamp: DateTime,
                          closeTimeStamp: DateTime,
                          currentStatus: String,
                          closeStatus: Option[String],
                          cancelRequested: Boolean = false)

  sealed abstract class Status(val value: String)

  object Status {

    case object WaitScheduled extends Status("WaitScheduled")

    case object Initialize extends Status("Initialize")

    case object Running extends Status("Running")

    case object Deleted extends Status("Deleted")

    case object Failed extends Status("Failed")

    case object Complete extends Status("Complete")

    def unapply(s: String): Option[Status] =
      s match {
        case "WaitScheduled" => Some(WaitScheduled)
        case "Initialize" => Some(Initialize)
        case "Running" => Some(Running)
        case "Deleted" => Some(Deleted)
        case "Failed" => Some(Failed)
        case "Complete" => Some(Complete)
        case _ => None
      }
  }

  case class Event(timestamp: DateTime, status: Status, details: Option[String])

  case class Instance(activityId: Option[String] = None,
                      runId: String,
                      activityType: Activity.Type,
                      workflowExecution: Option[Workflow.Instance] = None,
                      startedEventId: Option[String] = None,
                      scheduleToStart: Option[Duration] = Some(Duration.Inf),
                      scheduleToClose: Option[Duration] = Some(Duration.Inf),
                      priority: Int = 0,
                      heartbeatTimeout: Option[Duration] = Some(Duration.Inf),
                      startToCloseTimeout: Option[Duration] = Some(Duration.Inf),
                      history: List[Event] = Nil,
                      input: Option[String] = None,
                      output: Option[String] = None,
                      createTimeStamp: DateTime,
                      startTimeStamp: Option[DateTime] = None,
                      lastHeartBeatTimeStamp: Option[DateTime] = None,
                      closeTimeStamp: Option[DateTime] = None,
                      currentStatus: String,
                      closeStatus: Option[String] = None,
                      cancelRequested: Boolean = false)

}
