package swe.model


import com.github.nscala_time.time.Imports.DateTime
import scala.concurrent.duration._


sealed trait WorkflowEvent extends Types {
  def timestamp: DateTime
  def id: EventId
}

object WorkflowEvent extends Types {

  case class WorkflowExecutionStarted(timestamp: DateTime, id: EventId, details: WorkflowExecutionStarted.Details) extends WorkflowEvent

  object WorkflowExecutionStarted {

    case class Details(childPolicy: ChildPolicy,
                       taskList: TaskList,
                       workflow: WorkflowType,
                       input: Option[String],
                       continuedExecutionRunId: Option[RunId],
                       executionStartToCloseTimeout: Option[Duration],
                       parentInitiatedEventId: Option[EventId],
                       parentWorkflowExecution: Option[WorkflowExecution],
                       tags: List[String],
                       taskPriority: Option[Int],
                       taskStartToCloseTimeout: Option[Duration])

  }

  case class ActivityScheduled(activity: ActivityType,
                               timestamp: DateTime,
                               id: EventId,
                               details: ActivityScheduled.Details) extends WorkflowEvent

  object ActivityScheduled {

    case class Details(activityId: ActivityId, taskList: TaskList, taskPriority: Option[Int], control: Option[String],
                       input: Option[String], triggeringDecision: EventId, heartbeatTimeout: Option[Duration],
                       scheduleToCloseTimeout: Option[Duration], scheduleToStartTimeout: Option[Duration],
                       startToCloseTimeout: Option[Duration])

  }

  case class ActivityStarted(timestamp: DateTime,
                             id: EventId,
                             scheduledEventId: EventId,
                             scheduledEvent: Option[ActivityScheduled]) extends WorkflowEvent

  case class ActivityCompleted(scheduledEvent: Option[ActivityScheduled],
                               result: Option[String],
                               timestamp: DateTime,
                               id: EventId,
                               scheduledEventId: EventId) extends WorkflowEvent

  case class ActivityFailed(timestamp: DateTime,
                            id: EventId,
                            reason: Option[String],
                            detail: Option[String],
                            scheduledEventId: EventId,
                            scheduledEvent: Option[ActivityScheduled]) extends WorkflowEvent

  case class TimerStarted(timestamp: DateTime,
                          id: EventId,
                          timerId: TimerId,
                          timeout: FiniteDuration,
                          triggeringDecision: EventId,
                          control: Option[String]) extends WorkflowEvent

  case class TimerFired(startedEvent: Option[TimerStarted],
                        timestamp: DateTime,
                        id: EventId, timerId: TimerId,
                        startedEventId: EventId) extends WorkflowEvent

  case class UnknownEvent(timestamp: DateTime,
                          id: EventId,
                          event: HistoryEvent) extends WorkflowEvent

}
