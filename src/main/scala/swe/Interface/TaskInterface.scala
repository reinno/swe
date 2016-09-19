package swe.Interface

import swe.model._

object TaskInterface {
  /**
    * Http Url: v1/task/
    * Http Method: Post
    *
    * @return String task run_Id
    */
  case class PostTask(task_type: ActivityType,
                      report_endpoint: String,
                      timeout_seconds: String,
                      input: String)

  case class PostTaskStatus(closeStatus: String,
                            output: String)

  /**
    * Http Url: v1/task/{run_id}/heartbeat
    * Http Method: Post
    *
    * @param details details if something need log.
    * @return ActivityTaskStatus
    */
  case class PostTaskHeartBeat(details: String = "")
}
