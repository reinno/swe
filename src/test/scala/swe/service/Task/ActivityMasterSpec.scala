package swe.service.Task

import akka.testkit.TestProbe
import swe.model.Activity
import swe.service.BaseServiceHelper

class ActivityMasterSpec extends BaseServiceHelper.TestSpec {
  "ApiMaster" must {

    "post activity trigger notify" in {
      val apiMaster = TestProbe()

      val taskMaster = system.actorOf(ActivityMaster.props(apiMaster.ref))
      watch(taskMaster)

      val msg = ActivityMaster.PostTask(ActivityMaster.PostTask.Entity("demo", Some("v1.0")))
      taskMaster ! msg
      apiMaster.expectMsg(ActivityPoller.NewTaskNotify(Activity.Type("demo", Some("v1.0"))))
      expectMsgType[String]

      system.stop(taskMaster)
      expectTerminated(taskMaster)
    }

    "poll activity success" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      for (x <- List(activityType, Activity.Type("demo", None))) {
        val apiMaster = TestProbe()
        val activityMaster = system.actorOf(ActivityMaster.props(apiMaster.ref))
        watch(activityMaster)

        var runId: String = ""

        val msg = ActivityMaster.PostTask(ActivityMaster.PostTask.Entity(activityType.name, activityType.version))
        activityMaster ! msg
        apiMaster.expectMsg(ActivityPoller.NewTaskNotify(activityType))
        expectMsgPF() {
          case msg: String =>
            runId = msg
        }

        activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(x))
        expectMsgPF() {
          case msg: ActivityMaster.PollTasks.Response =>
            msg.instances.size shouldEqual 1
            msg.instances.head.activityType shouldBe activityType
            msg.instances.head.runId shouldBe runId
        }

        system.stop(activityMaster)
        expectTerminated(activityMaster)
      }
    }

    "get wait scheduled tasks success" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      val apiMaster = TestProbe()
      var runId: String = ""

      val taskMaster = system.actorOf(ActivityMaster.props(apiMaster.ref))
      watch(taskMaster)

      val msg = ActivityMaster.PostTask(ActivityMaster.PostTask.Entity(activityType.name, activityType.version))
      taskMaster ! msg
      apiMaster.expectMsg(ActivityPoller.NewTaskNotify(activityType))
      expectMsgPF() {
        case msg: String =>
          runId = msg
      }

      taskMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Activity.Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe Activity.Status.WaitScheduled.value
      }

      system.stop(taskMaster)
      expectTerminated(taskMaster)
    }
  }
}
