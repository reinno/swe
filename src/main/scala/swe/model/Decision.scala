package swe.model

import scala.concurrent.duration.{FiniteDuration, Duration}



object Decision {
  case class Instance(runId: String,
                      startedEventId: String,
                      workflowType: Workflow.Type,
                      workflowExecution: Workflow.Instance,
                      eventHistory: List[Workflow.Event])

  sealed trait Result
  object Result {
    case class ScheduleActivity(activity: Activity.Type,
                                id: String,
                                input: Option[String],
                                control: Option[String] = None,
                                scheduleToCloseTimeout: Option[Duration] = None,
                                taskList: Option[String] = None,
                                taskPriority: Option[Int] = None,
                                scheduleToStartTimeout: Option[Duration] = None,
                                startToCloseTimeout: Option[Duration] = None,
                                heartbeatTimeout: Option[Duration] = None) extends Result

    case class RequestCancelActivity(activityId: String) extends Result

    case class CompleteWorkflowExecution(result: Option[String]) extends Result

    case class FailWorkflowExecution(reason: String, detail: String) extends Result

    case class CancelWorkflowExecution(detail: String) extends Result

    case class ContinueAsNewWorkflowExecution(input: String,
                                              executionStartToCloseTimeout: Option[Duration] = None,
                                              taskList: String,
                                              taskPriority: Option[Int] = None,
                                              taskStartToCloseTimeout: Option[Duration] = None,
                                              childPolicy: Option[ChildPolicy] = None,
                                              tags: List[String] = Nil,
                                              workflowTypeVersion: Option[String] = None) extends Result

    case class RecordMarker(name: String, detail: String) extends Result

    case class StartTimer(timerId: String, startToFire: FiniteDuration, control: Option[String] = None)

    case class CancelTimer(timerId: String) extends Result

    case class SignalExternalWorkflowExecution(workflowId: String,
                                               runId: String,
                                               name: String,
                                               input: Option[String] = None,
                                               control: Option[String] = None) extends Result

    case class RequestCancelExternalWorkflowExecution(workflowId: String,
                                                      runId: String,
                                                      control: Option[String] = None) extends Result

    case class StartChildWorkflowExecution(workflowId: String,
                                           workflow: Workflow.Type,
                                           input: String,
                                           control: Option[String] = None,
                                           executionStartToCloseTimeout: Option[Duration] = None,
                                           taskList: Option[String] = None,
                                           taskPriority: Option[Int] = None,
                                           taskStartToCloseTimeout: Option[Duration] = None,
                                           childPolicy: Option[ChildPolicy] = None,
                                           tags: List[String] = Nil) extends Result
  }

}
