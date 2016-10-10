package swe.service.Task

import akka.testkit.TestProbe
import swe.model.Activity
import swe.service.BaseServiceHelper

class TaskMasterSpec extends BaseServiceHelper.TestSpec {
  "ApiMaster" must {

    "post task trigger notify" in {
      val apiMaster = TestProbe()

      val taskMaster = system.actorOf(TaskMaster.props(apiMaster.ref))
      watch(taskMaster)

      val msg = TaskMaster.PostTask(TaskMaster.PostTask.Entity("demo", Some("v1.0")))
      taskMaster ! msg
      apiMaster.expectMsg(TaskPoller.NewTaskNotify(Activity.Type("demo", Some("v1.0"))))
      expectMsgType[String]

      system.stop(taskMaster)
      expectTerminated(taskMaster)
    }

    "poll task success" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      for (x <- List(activityType, Activity.Type("demo", None))) {
        val apiMaster = TestProbe()
        val taskMaster = system.actorOf(TaskMaster.props(apiMaster.ref))
        watch(taskMaster)

        var runId: String = ""

        val msg = TaskMaster.PostTask(TaskMaster.PostTask.Entity(activityType.name, activityType.version))
        taskMaster ! msg
        apiMaster.expectMsg(TaskPoller.NewTaskNotify(activityType))
        expectMsgPF() {
          case msg: String =>
            runId = msg
        }

        taskMaster ! TaskMaster.PollTasks(TaskMaster.PollTasks.Entity(x))
        expectMsgPF() {
          case msg: TaskMaster.PollTasks.Response =>
            msg.instances.size shouldEqual 1
            msg.instances.head.activityType shouldBe activityType
            msg.instances.head.runId shouldBe runId
        }

        system.stop(taskMaster)
        expectTerminated(taskMaster)
      }
    }
  }
}
