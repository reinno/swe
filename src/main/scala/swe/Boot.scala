package swe


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import swe.model.Activity
import swe.route.ApiRouteService
import swe.service.{DbService, ApiMaster, HttpClientService, HttpClientSingle}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Boot extends App {
  implicit val system = ActorSystem("swe")
  implicit val timeout = Timeout(10 seconds)
  implicit val mat = ActorMaterializer()

  val settings = Settings(system)
  implicit val httpClientSingleFactory: HttpClientService.HttpClientFactory =
    () => new HttpClientSingle
  val apiMaster = system.actorOf(ApiMaster.props(), "ApiMaster")


  val service = new ApiRouteService(apiMaster)

  DbService.init(List(Activity.Instance.InstancesDao))


  val bindFuture = Http().bindAndHandle(Route.handlerFlow(service.route),
    settings.bindAddr, settings.bindPort)

  Await.result(bindFuture, 15 seconds)
  Await.result(system.whenTerminated, Duration.Inf)
}
