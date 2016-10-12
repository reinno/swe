package swe.service

import akka.actor.Props
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import swe.service.Task.ActivityMaster

class ApiMasterSpec extends BaseServiceHelper.TestSpec {
  "ApiMaster" must {
    implicit val mat = ActorMaterializer()
    implicit val httpClientSingleFactory: HttpClientService.HttpClientFactory =
      () => new HttpClientSingle

    "init success" in {
      val apiMaster = system.actorOf(ApiMaster.props(), "ApiMaster")
      watch(apiMaster)

      apiMaster ! "test"
      expectMsg("unknown msg")
      system.stop(apiMaster)
      expectTerminated(apiMaster)
    }

    "transfer task master msg" in {
      val taskMasterProbe = TestProbe()
      class ApiMasterExtend extends ApiMaster {
        override val activityMaster = taskMasterProbe.ref
      }

      val apiMaster = system.actorOf(Props(new ApiMasterExtend()), "ApiMaster")
      watch(apiMaster)

      val msg = ActivityMaster.DeleteTask("1")
      apiMaster ! msg
      taskMasterProbe.expectMsg(msg)
      system.stop(apiMaster)
      expectTerminated(apiMaster)
    }
  }
}
