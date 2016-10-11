package swe


import swe.route.ApiRouteService
import swe.service.{HttpClientSingle, HttpClientService, ApiMaster}
import swe.util.Constants

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.language.postfixOps

class Boot {
  implicit val system = ActorSystem("swe")
  implicit val timeout = Timeout(10 seconds)
  implicit val mat = ActorMaterializer()

  val settings = Settings(system)
  implicit val httpClientSingleFactory: HttpClientService.HttpClientFactory =
    () => new HttpClientSingle
  val apiMaster = system.actorOf(ApiMaster.props(), "ApiMaster")


  val service = new ApiRouteService(apiMaster)


  val bindFuture = Http().bindAndHandle(Route.handlerFlow(service.route),
    settings.bindAddr, settings.bindPort)

  Await.result(bindFuture, 15 seconds)
  Await.result(system.whenTerminated, Duration.Inf)
}
