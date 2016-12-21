package swe.service.Task

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.testkit.TestProbe
import swe.model.Activity
import swe.model.Activity.Instance
import swe.service.BaseServiceHelper
import swe.service.Task.ActivityMaster.GetTasks
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

    def checkActivityStatus(activityMaster: ActorRef, runId: String, activityType: Activity.Type,
                            currentStatus: String, closeStatus: Option[String]): Unit = {
      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe activityType
          msg.currentStatus shouldBe currentStatus
          msg.closeStatus shouldBe closeStatus
      }
    }

    val defaultActivityType = Activity.Type("demo", Some("v1.0"))
    "post activity trigger notify" in {
      val activityType = defaultActivityType
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(activityType, apiMaster)

      postProc(activityMaster)
    }

    "poll activity success" in {
      val apiMaster = TestProbe()
      for (x <- List(defaultActivityType, Activity.Type("demo", None))) {
        val activityMaster: ActorRef =
          preProc(defaultActivityType, apiMaster)

        activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(x))
        expectMsgPF() {
          case msg: Response =>
            msg.instances.size shouldEqual 1
            msg.instances.head.activityType shouldBe defaultActivityType
            msg.instances.head.runId shouldBe runId
        }

        postProc(activityMaster)
      }
    }

    "poll multie activity 1 by 1 success" in {
      val apiMaster = TestProbe()

      val activityType = defaultActivityType

      val activityMaster: ActorRef =
        preProc(activityType, apiMaster)


      var runId2 = ""
      val msg = ActivityMaster.PostTask(ActivityMaster.PostTaskEntity(activityType.name, activityType.version))
      activityMaster ! msg
      apiMaster.expectMsg(ActivityPoller.NewTaskNotify(activityType))
      expectMsgPF() {
        case msg: String =>
          runId2 = msg
      }

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType, 10))
      expectMsgPF() {
        case msg: Response =>
          msg.instances.size shouldEqual 2
          msg.instances.head.activityType shouldBe defaultActivityType
      }

      postProc(activityMaster)
    }


    "poll multie activity success" in {
      val apiMaster = TestProbe()

      val activityType = defaultActivityType

      val activityMaster: ActorRef =
        preProc(activityType, apiMaster)


      var runId2 = ""
      val msg = ActivityMaster.PostTask(ActivityMaster.PostTaskEntity(activityType.name, activityType.version))
      activityMaster ! msg
      apiMaster.expectMsg(ActivityPoller.NewTaskNotify(activityType))
      expectMsgPF() {
        case msg: String =>
          runId2 = msg
      }

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType))
      expectMsgPF() {
        case msg: Response =>
          println("check1st")
          msg.instances.size shouldEqual 1
          msg.instances.head.activityType shouldBe defaultActivityType
          msg.instances.head.runId shouldBe runId
      }

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType))
      expectMsgPF() {
        case msg: Response =>
          println("check2nd")
          msg.instances.size shouldEqual 1
          msg.instances.head.activityType shouldBe defaultActivityType
          msg.instances.head.runId shouldBe runId2
      }

      postProc(activityMaster)
    }

    "get wait scheduled tasks success" in {
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(defaultActivityType, apiMaster)

      checkActivityStatus(activityMaster, runId, defaultActivityType, Activity.Status.WaitScheduled.value, None)

      postProc(activityMaster)
    }

    "heartbeat timeout trigger activity failure" in {
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(defaultActivityType, apiMaster)

      checkActivityStatus(activityMaster, runId, defaultActivityType, Activity.Status.WaitScheduled.value, None)

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(defaultActivityType))
      expectMsgPF() {
        case msg: Response =>
          msg.instances.size shouldEqual 1
          msg.instances.head.activityType shouldBe defaultActivityType
          msg.instances.head.runId shouldBe runId
      }

      //default timeout = 5s + 1s + 2s
      expectNoMsg(10 seconds)

      val timeoutStatus = Activity.Status.Timeout.value
      checkActivityStatus(activityMaster, runId, defaultActivityType, timeoutStatus, Some(timeoutStatus))

      val msg = ActivityMaster.PostTask(ActivityMaster.PostTaskEntity(defaultActivityType.name,
        defaultActivityType.version, heartbeatTimeout = Some(12)))
      activityMaster ! msg
      apiMaster.expectMsg(ActivityPoller.NewTaskNotify(defaultActivityType))
      expectMsgPF() {
        case msg: String =>
          runId = msg
      }

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(defaultActivityType))
      expectMsgPF() {
        case msg: Response =>
          msg.instances.size shouldEqual 1
          msg.instances.head.activityType shouldBe defaultActivityType
          msg.instances.head.runId shouldBe runId
      }

      expectNoMsg(10 seconds)
      checkActivityStatus(activityMaster, runId, defaultActivityType, Activity.Status.Initialize.value, None)

      expectNoMsg(6 seconds)
      checkActivityStatus(activityMaster, runId, defaultActivityType, timeoutStatus, Some(timeoutStatus))

      postProc(activityMaster)
    }

    "post heartbeat" in {
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(defaultActivityType, apiMaster)

      activityMaster ! ActivityMaster.GetTask(runId)
      expectMsgPF() {
        case Some(msg: Instance) =>
          msg.runId shouldBe runId
          msg.activityType shouldBe defaultActivityType
          msg.currentStatus shouldBe Activity.Status.WaitScheduled.value
      }

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(defaultActivityType))
      expectMsgPF() {
        case msg: Response =>
          msg.instances.size shouldEqual 1
          msg.instances.head.activityType shouldBe defaultActivityType
          msg.instances.head.runId shouldBe runId
      }

      expectNoMsg(4 seconds)
      activityMaster ! ActivityMaster.PostTaskHeartBeat(runId, ActivityMaster.PostTaskHeartBeat.Entity(Some("demo")))
      expectMsg(StatusCodes.OK)
      expectNoMsg(4 seconds)

      checkActivityStatus(activityMaster, runId, defaultActivityType, Activity.Status.Running.value, None)
      postProc(activityMaster)
    }

    "get task is sorted in reversed order" in {
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(defaultActivityType, apiMaster)

      var runId2 = ""
      val msg = ActivityMaster.PostTask(ActivityMaster.PostTaskEntity("aaa", defaultActivityType.version))
      activityMaster ! msg
      apiMaster.expectMsgType[ActivityPoller.NewTaskNotify]
      expectMsgPF() {
        case msg: String =>
          runId2 = msg
      }

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
      val input = "demo input"
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(defaultActivityType, apiMaster, Some(input))

      checkActivityStatus(activityMaster, runId, defaultActivityType, Activity.Status.WaitScheduled.value, None)

      postProc(activityMaster)
    }


    "activity complete" in {
      val apiMaster = TestProbe()
      val activityMaster: ActorRef =
        preProc(defaultActivityType, apiMaster)


      checkActivityStatus(activityMaster, runId, defaultActivityType, Activity.Status.WaitScheduled.value, None)

      activityMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(defaultActivityType))
      expectMsgPF() {
        case msg: Response =>
          msg.instances.size shouldEqual 1
          msg.instances.head.activityType shouldBe defaultActivityType
          msg.instances.head.runId shouldBe runId
      }

      val status = Activity.Status.Complete.value
      activityMaster ! ActivityMaster.PostTaskStatus(runId, ActivityMaster.PostTaskStatus.Entity(status))
      expectMsg(StatusCodes.OK)

      activityMaster ! ActivityMaster.GetTasks
      expectMsgPF() {
        case msg: GetTasks.Response =>
          msg.instances.size shouldBe 1
      }

      checkActivityStatus(activityMaster, runId, defaultActivityType, status, Some(status))
      postProc(activityMaster)
    }
  }
}
