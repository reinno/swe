package swe.interface

import swe.model._

object TaskInterface {
  /**
    * Http Url: v1/task/
    * Http Method: Post
    *
    * @return String task run_Id
    */
  case class PostTask(task_type: Activity.Type,
                      report_endpoint: String,
                      timeout_seconds: String,
                      input: Option[String])

  case class PostTaskStatus(closeStatus: String,
                            details: Option[String] = None,
                            output: Option[String] = None)

  /**
    * Http Url: v1/task/{run_id}/heartbeat
    * Http Method: Post
    *
    * @param details details if something need log.
    * @return
    */
  case class PostTaskHeartBeat(details: Option[String] = None)
}
