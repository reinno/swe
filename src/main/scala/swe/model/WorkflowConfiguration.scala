package swe.model

case class WorkflowConfiguration(startToEndTimeoutSec: Int,
                                 taskStartToEndTimeoutSec: Option[Int],
                                 taskPriority: Option[String])
