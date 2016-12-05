package swe.service.Task

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestProbe

import swe.model.Activity
import swe.service.{HttpClientService, BaseServiceHelper}
import swe.service.HttpClientServiceHelper.HttpClientSenderDummy

import scala.concurrent.duration._
import scala.language.postfixOps

class ActivityPollerSpecClient(implicit val mat: Materializer, val system: ActorSystem) extends HttpClientSenderDummy

class ActivityPollerSpec extends BaseServiceHelper.TestSpec {
  implicit val mat: Materializer = ActorMaterializer()
  implicit val httpClientSingleFactory: HttpClientService.HttpClientFactory = {
    () => new ActivityPollerSpecClient()
  }

  "An Activity Poller actor" must {
    "poll same task with interval" in {
      val apiMaster = TestProbe()
      val activityPoller = system.actorOf(ActivityPoller.props(apiMaster.ref))

      val activityType = Activity.Type("demo")
      for (i <- 1 to 2) {
        apiMaster.send(activityPoller, ActivityPoller.NewTaskNotify(activityType))
      }

      apiMaster.expectMsg(6 seconds, ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType)))
      println("wait")
      apiMaster.expectNoMsg(2 seconds)
      apiMaster.expectMsg(ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType)))
    }

    "poll diff task with interval" in {
      val apiMaster = TestProbe()
      val activityPoller = system.actorOf(ActivityPoller.props(apiMaster.ref))

      val activityType1 = Activity.Type("demo1")
      val activityType2 = Activity.Type("demo2")
      apiMaster.send(activityPoller, ActivityPoller.NewTaskNotify(activityType1))
      apiMaster.send(activityPoller, ActivityPoller.NewTaskNotify(activityType2))


      apiMaster.expectMsg(6 seconds, ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType1)))
      println("wait")
      apiMaster.expectNoMsg(2 seconds)
      apiMaster.expectMsg(ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType2)))
    }
  }
}
