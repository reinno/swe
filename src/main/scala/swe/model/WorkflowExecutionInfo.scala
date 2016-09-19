package swe.model

case class WorkflowExecutionInfo(execution: WorkflowExecution,
                                 workflowType: WorkflowType,
                                 startTimeStamp: String,
                                 closeTimeStamp: String,
                                 currentStatus: String,
                                 closeStatus: String,
                                 parent: WorkflowExecution,
                                 cancelRequested: Boolean)
