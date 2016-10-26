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


    def preProc(activityType: Activity.Type, apiMaster: TestProbe, input: Option[String] = None): ActorRef = {
      val activityMaster = system.actorOf(ActivityMaster.props(apiMaster.ref))
      watch(activityMaster)
      val msg = ActivityMaster.PostTask(ActivityMaster.PostTaskEntity(activityType.name, activityType.version, input = input))
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
      expectNoMsg(4 seconds)

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

    "get task is sorted in reversed order" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(activityType, apiMaster)

      var runId2 = ""
      val msg = ActivityMaster.PostTask(ActivityMaster.PostTaskEntity("aaa", activityType.version))
      activityMaster ! msg
      apiMaster.expectMsgType[ActivityPoller.NewTaskNotify]
      expectMsgPF() {
        case msg: String =>
          runId2 = msg
      }
      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(Activity.Type("aaa", None)))
      expectMsgType[ActivityMaster.PollTasks.Response]

      activityMaster ! ActivityMaster.GetTasks
      expectMsgPF() {
        case msg: ActivityMaster.GetTasks.Response =>
          msg.instances.size shouldBe 2
          msg.instances.head.runId shouldBe runId2
          msg.instances.drop(1).head.runId shouldBe runId
      }

      postProc(activityMaster)
    }

    "record input" in {
      val activityType = Activity.Type("demo", Some("v1.0"))
      val input = "demo input"
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(activityType, apiMaster, Some(input))

      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe Activity.Status.WaitScheduled.value
          msg.input shouldBe Some(input)
      }

      postProc(activityMaster)
    }


    "activity complete" in {
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

      val status = Activity.Status.Complete.value
      activityMaster ! ActivityMaster.PostTaskStatus(runId, ActivityMaster.PostTaskStatus.Entity(status))
      expectMsg(StatusCodes.OK)

      activityMaster ! ActivityMaster.GetTasks
      expectMsgPF() {
        case msg: ActivityMaster.GetTasks.Response =>
          msg.instances.size shouldBe 1
      }

      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe status
          msg.closeStatus shouldBe Some(status)
      }
      postProc(activityMaster)
    }
  }
}
