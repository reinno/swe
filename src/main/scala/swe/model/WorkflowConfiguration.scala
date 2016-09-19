package swe.model

import scala.concurrent.duration.Duration

case class WorkflowConfiguration(description: String,
                                 defaultTaskList: String,
                                 childPolicy: Option[ChildPolicy] = Some(ChildPolicy.Terminate),
                                 defaultTaskPriority: Int = 0,
                                 defaultExecutionStartToCloseTimeout: Duration,
                                 defaultTaskStartToCloseTimeout: Option[Duration] = Some(Duration.Inf))
