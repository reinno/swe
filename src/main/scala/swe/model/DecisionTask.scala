package swe.model

case class DecisionTask(runId: String,
                        startedEventId: String,
                        workflowType: WorkflowType,
                        workflowExecution: WorkflowExecution,
                        eventHistory: List[HistoryEvent])
