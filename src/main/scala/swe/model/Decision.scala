package swe.model

import scala.concurrent.duration.{FiniteDuration, Duration}


sealed trait Decision

object Decision {
  case class ScheduleActivity(activity: ActivityType,
                              id: String,
                              input: Option[String],
                              control: Option[String] = None,
                              scheduleToCloseTimeout: Option[Duration] = None,
                              taskList: Option[String] = None,
                              taskPriority: Option[Int] = None,
                              scheduleToStartTimeout: Option[Duration] = None,
                              startToCloseTimeout: Option[Duration] = None,
                              heartbeatTimeout: Option[Duration] = None) extends Decision

  case class RequestCancelActivity(activityId: String) extends Decision

  case class CompleteWorkflowExecution(result: Option[String]) extends Decision

  case class FailWorkflowExecution(reason: String, detail: String) extends Decision

  case class CancelWorkflowExecution(detail: String) extends Decision

  case class ContinueAsNewWorkflowExecution(input: String,
                                            executionStartToCloseTimeout: Option[Duration] = None,
                                            taskList: String,
                                            taskPriority: Option[Int] = None,
                                            taskStartToCloseTimeout: Option[Duration] = None,
                                            childPolicy: Option[ChildPolicy] = None,
                                            tags: List[String] = Nil,
                                            workflowTypeVersion: Option[String] = None) extends Decision

  case class RecordMarker(name: String, detail: String) extends Decision

  case class StartTimer(timerId: String, startToFire: FiniteDuration, control: Option[String] = None)

  case class CancelTimer(timerId: String) extends Decision

  case class SignalExternalWorkflowExecution(workflowId: String,
                                             runId: String,
                                             name: String,
                                             input: Option[String] = None,
                                             control: Option[String] = None) extends Decision

  case class RequestCancelExternalWorkflowExecution(workflowId: String,
                                                    runId: String,
                                                    control: Option[String] = None) extends Decision

  case class StartChildWorkflowExecution(workflowId: String,
                                         workflow: WorkflowType,
                                         input: String,
                                         control: Option[String] = None,
                                         executionStartToCloseTimeout: Option[Duration] = None,
                                         taskList: Option[String] = None,
                                         taskPriority: Option[Int] = None,
                                         taskStartToCloseTimeout: Option[Duration] = None,
                                         childPolicy: Option[ChildPolicy] = None,
                                         tags: List[String] = Nil) extends Decision
}
