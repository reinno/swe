package swe.service

import akka.actor.Props
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import swe.service.Task.TaskMaster

class ApiMasterSpec extends BaseServiceHelper.TestSpec {
  "ApiMaster" must {
    implicit val mat = ActorMaterializer()
    val httpClientSingleFactory: HttpClientService.HttpClientFactory =
      () => new HttpClientSingle

    "init success" in {
      val apiMaster = system.actorOf(ApiMaster.props(httpClientSingleFactory), "ApiMaster")
      watch(apiMaster)

      apiMaster ! "test"
      expectMsg("unknown msg")
      system.stop(apiMaster)
      expectTerminated(apiMaster)
    }

    "transfer task master msg" in {
      val taskMasterProbe = TestProbe()
      class ApiMasterExtend(httpClientFactory: HttpClientService.HttpClientFactory) extends ApiMaster(httpClientFactory) {
        override val taskMaster = taskMasterProbe.ref
      }

      val apiMaster = system.actorOf(Props(new ApiMasterExtend(httpClientSingleFactory)), "ApiMaster")
      watch(apiMaster)

      val msg = TaskMaster.DeleteTask("1")
      apiMaster ! msg
      taskMasterProbe.expectMsg(msg)
      system.stop(apiMaster)
      expectTerminated(apiMaster)
    }
  }
}
