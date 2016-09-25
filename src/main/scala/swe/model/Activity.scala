package swe.model

import scala.concurrent.duration.Duration

object Activity {

  case class Type(name: String, version: String)

  case class Configuration(description: String,
                           defaultTaskList: String,
                           defaultTaskScheduleToStart: Option[Duration] = Some(Duration.Inf),
                           defaultTaskScheduleToClose: Option[Duration] = Some(Duration.Inf),
                           defaultTaskPriority: Int = 0,
                           defaultTaskHeartbeatTimeout: Option[Duration] = Some(Duration.Inf),
                           defaultTaskStartToCloseTimeout: Option[Duration] = Some(Duration.Inf))


  case class Instance(activityId: String,
                      runId: String,
                      activityType: Activity.Type,
                      workflowExecution: Option[Workflow.Instance],
                      startedEventId: String,
                      input: Option[String])


  case class InstanceInfo(execution: Option[Workflow.Instance],
                          activityType: Activity.Type,
                          startTimeStamp: String,
                          lastHeartBeatTimeStamp: String,
                          closeTimeStamp: String,
                          currentStatus: String,
                          closeStatus: String,
                          cancelRequested: Boolean)

}
