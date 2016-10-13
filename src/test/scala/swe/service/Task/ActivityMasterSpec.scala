package swe.service.Task

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.testkit.TestProbe
import swe.model.Activity
import swe.model.Activity.Instance
import swe.service.BaseServiceHelper
import swe.service.Task.ActivityMaster.PollTasks.Response
import scala.concurrent.duration._
import scala.language.postfixOps

class ActivityMasterSpec extends BaseServiceHelper.TestSpec {
  "activity master" must {

    var runId: String = ""

    def preProc(activityType: Activity.Type, apiMaster: TestProbe): ActorRef = {
      val activityMaster = system.actorOf(ActivityMaster.props(apiMaster.ref))
      watch(activityMaster)

      val msg = ActivityMaster.PostTask(ActivityMaster.PostTask.Entity(activityType.name, activityType.version))
      activityMaster ! msg
      apiMaster.expectMsg(ActivityPoller.NewTaskNotify(activityType))
      expectMsgPF() {
        case msg: String =>
          runId = msg
      }
      activityMaster
    }

    def postProc(activityMaster: ActorRef): Unit = {
      system.stop(activityMaster)
      expectTerminated(activityMaster)
      runId = ""
    }

    "post activity trigger notify" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(activityType, apiMaster)

      postProc(activityMaster)
    }

    "poll activity success" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      val apiMaster = TestProbe()
      for (x <- List(activityType, Activity.Type("demo", None))) {
        val activityMaster: ActorRef =
          preProc(activityType, apiMaster)

        activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(x))
        expectMsgPF() {
          case msg: Response =>
            msg.instances.size shouldEqual 1
            msg.instances.head.activityType shouldBe activityType
            msg.instances.head.runId shouldBe runId
        }

        postProc(activityMaster)
      }
    }

    "get wait scheduled tasks success" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(activityType, apiMaster)


      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe Activity.Status.WaitScheduled.value
      }

      postProc(activityMaster)
    }

    "heartbeat timeout trigger activity failure" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(activityType, apiMaster)

      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe Activity.Status.WaitScheduled.value
      }

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType))
      expectMsgPF() {
        case msg: Response =>
          msg.instances.size shouldEqual 1
          msg.instances.head.activityType shouldBe activityType
          msg.instances.head.runId shouldBe runId
      }

      expectNoMsg(12 seconds)
      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe Activity.Status.Timeout.value
          msg.closeStatus shouldBe Some(Activity.Status.Timeout.value)
      }

      postProc(activityMaster)
    }

    "post heartbeat" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(activityType, apiMaster)

      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe Activity.Status.WaitScheduled.value
      }

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType))
      expectMsgPF() {
        case msg: Response =>
          msg.instances.size shouldEqual 1
          msg.instances.head.activityType shouldBe activityType
          msg.instances.head.runId shouldBe runId
      }

      expectNoMsg(4 seconds)
      activityMaster ! ActivityMaster.PostTaskHeartBeat(runId, ActivityMaster.PostTaskHeartBeat.Entity(Some("demo")))
      expectMsg(StatusCodes.OK)
      expectNoMsg(6 seconds)

      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe Activity.Status.Running.value
          msg.closeStatus shouldBe None
      }
      postProc(activityMaster)
    }
  }
}
