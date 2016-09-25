package swe.interface

import swe.model._

object WorkflowInterface {
  /**
    * Http Url: v1/workflow/execution/history
    * Http Method: Get
   * @return
    */
  case class GetWorkflowExecutionHistory(domain: String,
                                         workflowExecution: Workflow.Instance,
                                         nextPageToken: String = "",
                                         maxPageSize: Int = 1000,
                                         reserveOrder: Boolean = true)
  /**
    * Http Url: v1/workflow/activity/poll
    * Http Method: Post
    * @return Activity.Instance
    */
  case class PollActivityTask(domain: String,
                              taskList: String,
                              workerId: String)

  /**
    * Http Url: v1/workflow/decision/poll
    * Http Method: Post
    * @return Decision.Instacne
    */
  case class PollDecisionTask(domain: String,
                              taskList: String,
                              workerId: String)

  /**
    * Http Url: /workflow/activity/{run_id}/heartbeat
    * Http Method: Post
    * @param details details if something need log.
    * @return
    */
  case class PostActivityTaskHeartBeat(details: String = "")
}
