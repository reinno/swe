package swe.model

import scala.concurrent.duration.{FiniteDuration, Duration}
import com.github.nscala_time.time.Imports.DateTime

object Workflow {
  case class Type(name: String, version: String)

  case class Configuration(description: String,
                           defaultTaskList: String,
                           childPolicy: Option[ChildPolicy] = Some(ChildPolicy.Terminate),
                           defaultTaskPriority: Int = 0,
                           defaultExecutionStartToCloseTimeout: Duration,
                           defaultTaskStartToCloseTimeout: Option[Duration] = Some(Duration.Inf))

  case class Instance(workflowId: String, runId: String)

  case class InstanceInfo(execution: Workflow.Instance,
                          workflowType: Workflow.Type,
                          startTimeStamp: String,
                          closeTimeStamp: String,
                          currentStatus: String,
                          closeStatus: String,
                          parent: Workflow.Instance,
                          cancelRequested: Boolean)

  sealed trait Event extends Types {
    def timestamp: DateTime
    def id: EventId
  }

  object Event extends Types {

    case class WorkflowExecutionStarted(timestamp: DateTime, id: EventId, details: WorkflowExecutionStarted.Details) extends Event

    object WorkflowExecutionStarted {

      case class Details(childPolicy: ChildPolicy,
                         taskList: TaskList,
                         workflow: Workflow.Type,
                         input: Option[String],
                         continuedExecutionRunId: Option[RunId],
                         executionStartToCloseTimeout: Option[Duration],
                         parentInitiatedEventId: Option[EventId],
                         parentWorkflowExecution: Option[Workflow.Instance],
                         tags: List[String],
                         taskPriority: Option[Int],
                         taskStartToCloseTimeout: Option[Duration])

    }

    case class ActivityScheduled(activity: Activity.Type,
                                 timestamp: DateTime,
                                 id: EventId,
                                 details: ActivityScheduled.Details) extends Event

    object ActivityScheduled {

      case class Details(activityId: ActivityId, taskList: TaskList, taskPriority: Option[Int], control: Option[String],
                         input: Option[String], triggeringDecision: EventId, heartbeatTimeout: Option[Duration],
                         scheduleToCloseTimeout: Option[Duration], scheduleToStartTimeout: Option[Duration],
                         startToCloseTimeout: Option[Duration])

    }

    case class ActivityStarted(timestamp: DateTime,
                               id: EventId,
                               scheduledEventId: EventId,
                               scheduledEvent: Option[ActivityScheduled]) extends Event

    case class ActivityCompleted(scheduledEvent: Option[ActivityScheduled],
                                 result: Option[String],
                                 timestamp: DateTime,
                                 id: EventId,
                                 scheduledEventId: EventId) extends Event

    case class ActivityFailed(timestamp: DateTime,
                              id: EventId,
                              reason: Option[String],
                              detail: Option[String],
                              scheduledEventId: EventId,
                              scheduledEvent: Option[ActivityScheduled]) extends Event

    case class TimerStarted(timestamp: DateTime,
                            id: EventId,
                            timerId: TimerId,
                            timeout: FiniteDuration,
                            triggeringDecision: EventId,
                            control: Option[String]) extends Event

    case class TimerFired(startedEvent: Option[TimerStarted],
                          timestamp: DateTime,
                          id: EventId, timerId: TimerId,
                          startedEventId: EventId) extends Event

    case class UnknownEvent(timestamp: DateTime,
                            id: EventId) extends Event

  }
}
