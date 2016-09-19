package swe.model

import scala.concurrent.duration.Duration

case class ActivityConfiguration(description: String,
                                 defaultTaskList: String,
                                 defaultTaskScheduleToStart: Option[Duration] = Some(Duration.Inf),
                                 defaultTaskScheduleToClose: Option[Duration] = Some(Duration.Inf),
                                 defaultTaskPriority: Int = 0,
                                 defaultTaskHeartbeatTimeout: Option[Duration] = Some(Duration.Inf),
                                 defaultTaskStartToCloseTimeout: Option[Duration] = Some(Duration.Inf))
