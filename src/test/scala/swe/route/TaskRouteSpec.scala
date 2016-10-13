package swe.route

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestActor.{AutoPilot, KeepRunning}
import akka.testkit.TestProbe
import akka.util.ByteString
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import swe.model.Activity
import swe.service.Task.ActivityMaster

import scala.concurrent.duration.Duration

class TaskRouteSpec extends FlatSpec with ScalatestRouteTest with Matchers with BeforeAndAfterAll {

  val apiMasterProbe = TestProbe()
  apiMasterProbe.setAutoPilot(new AutoPilot {
    override def run(sender: ActorRef, msg: Any): AutoPilot = {
      msg match {
        case _ =>
          KeepRunning
      }
    }
  })

  val route = new TaskRoute(apiMasterProbe.ref).route

  it should "handle delete task success" in {
    val runId = "111"

    apiMasterProbe.setAutoPilot(new AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        msg match {
          case msg: ActivityMaster.DeleteTask =>
            msg shouldBe ActivityMaster.DeleteTask(runId)
            sender ! StatusCodes.OK
            KeepRunning
        }
      }
    })

    Delete(s"/api/v1/task/$runId") ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  it should "handle get task success" in {
    val runId = "111"

    apiMasterProbe.setAutoPilot(new AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        msg match {
          case msg: ActivityMaster.GetTask =>
            msg shouldBe ActivityMaster.GetTask(runId)
            sender ! Some(Activity.Instance(runId = runId,
              activityType = Activity.Type("demoTask"),
              heartbeatTimeout = Duration(3, "min"),
              createTimeStamp = DateTime.now,
              currentStatus = Activity.Status.WaitScheduled.value))
            KeepRunning
        }
      }
    })

    Get(s"/api/v1/task/$runId") ~> route ~> check {
      print(response.entity)
      status shouldBe StatusCodes.OK
    }
  }

  it should "handle post task success" in {

    apiMasterProbe.setAutoPilot(new AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        msg match {
          case msg: ActivityMaster.PostTask =>
            msg.entity shouldBe ActivityMaster.PostTaskEntity("test")
            sender ! "1"
            KeepRunning
        }
      }
    })

    val postRequest = HttpRequest(
      HttpMethods.POST,
      uri = "/api/v1/task",
      entity = HttpEntity(MediaTypes.`application/json`, ByteString("""{"name":"test"}""")))

    postRequest ~> route ~> check {
      print(response.entity)
      status shouldBe StatusCodes.OK
    }
  }

  it should "handle post task heartbeat success" in {

    apiMasterProbe.setAutoPilot(new AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        msg match {
          case msg: ActivityMaster.PostTaskHeartBeat =>
            msg.entity shouldBe ActivityMaster.PostTaskHeartBeat.Entity(Some("test"))
            sender ! StatusCodes.OK
            KeepRunning
        }
      }
    })

    val postRequest = HttpRequest(
      HttpMethods.POST,
      uri = "/api/v1/task/1/heartbeat",
      entity = HttpEntity(MediaTypes.`application/json`,  ByteString("""{"details":"test"}""")))

    postRequest ~> route ~> check {
      print(response.entity)
      status shouldBe StatusCodes.OK
    }
  }


  it should "handle post task status success" in {

    apiMasterProbe.setAutoPilot(new AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        msg match {
          case msg: ActivityMaster.PostTaskStatus =>
            msg.entity shouldBe ActivityMaster.PostTaskStatus.Entity("Complete", Some("test"), Some("1"))
            sender ! StatusCodes.OK
            KeepRunning
        }
      }
    })

    val jsonRequest = ByteString(
      s"""
         |{
         |    "status":"Complete",
         |    "details":"test",
         |    "output":"1"
         |}
        """.stripMargin)
    val postRequest = HttpRequest(
      HttpMethods.POST,
      uri = "/api/v1/task/1/status",
      entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

    postRequest ~> route ~> check {
      print(response.entity)
      status shouldBe StatusCodes.OK
    }
  }
}
