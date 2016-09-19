package swe.model

case class ActivityTask(activityId: String,
                        runId: String,
                        activityType: ActivityType,
                        workflowExecution: Option[WorkflowExecution],
                        startedEventId: String,
                        input: Option[String])
