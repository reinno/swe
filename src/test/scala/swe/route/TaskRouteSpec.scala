package swe.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestActor.{KeepRunning, AutoPilot}
import akka.testkit.TestProbe
import org.joda.time.DateTime
import org.scalatest.tools.Durations
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import swe.model.Activity
import swe.service.Task.TaskMaster

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
          case msg: TaskMaster.DeleteTask =>
            msg shouldBe TaskMaster.DeleteTask(runId)
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
          case msg: TaskMaster.GetTask =>
            msg shouldBe TaskMaster.GetTask(runId)
            sender ! Some(Activity.Instance(runId = runId,
              activityType = Activity.Type("demoTask"),
              heartbeatTimeout = Some(Duration(3, "min")),
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

}
