package swe.service

import akka.stream.ActorMaterializer

class ApiMasterSpec extends BaseServiceHelper.TestSpec {
  "ApiMaster" must {
    "init success" in {
      implicit val mat = ActorMaterializer()

      val httpClientSingleFactory: HttpClientService.HttpClientFactory =
        () => new HttpClientSingle
      val apiMaster = system.actorOf(ApiMaster.props(httpClientSingleFactory), "ApiMaster")

      apiMaster ! "test"
      expectMsg("unknown msg")
    }
  }
}
