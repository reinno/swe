package swe.service.Task

import akka.testkit.TestProbe
import swe.Settings
import swe.model.Activity
import swe.service.BaseServiceHelper

class ActivityPassivePollerSpec extends BaseServiceHelper.TestSpec {
  val settings = Settings(system)
  "activity passive poller" should {
    val apiMaster = TestProbe()

    "poll claiming success" in {
      val testType = Activity.Type("demo")
      val claimingRequest = ActivityMaster.PollTasks.Entity(testType, 1)
      val pollerActor = system.actorOf(ActivityPassivePoller.props(apiMaster.ref))

      pollerActor ! ActivityPassivePoller.StoreClaiming(claimingRequest)

      apiMaster.expectMsgPF() {
        case ActivityMaster.PollTasks(entity) =>
          entity shouldBe claimingRequest
      }


      apiMaster.expectMsgPF() {
        case ActivityMaster.PollTasks(entity) =>
          entity shouldBe claimingRequest
      }

      val result = List(Activity.InstanceInput(None, "111", testType))
      apiMaster.lastSender ! result

      expectMsg(result)
    }

    "poll claiming timeout" in {
      val testType = Activity.Type("demo")
      val claimingRequest = ActivityMaster.PollTasks.Entity(testType, 1)
      val pollerActor = system.actorOf(ActivityPassivePoller.props(apiMaster.ref))

      pollerActor ! ActivityPassivePoller.StoreClaiming(claimingRequest)

      expectNoMsg(settings.activityLongPollTimeout)

      expectMsg(Nil)
    }
  }
}
