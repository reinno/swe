package swe


import swe.route.ApiRouteService
import swe.service.ApiMaster
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

  val apiMaster = system.actorOf(Props[ApiMaster], "ApiMaster")

  implicit val mat = ActorMaterializer()
  val service = new ApiRouteService(system, apiMaster)


  val bindFuture = Http().bindAndHandle(Route.handlerFlow(service.route),
    Constants.SERVER_HOST, Constants.SERVER_PORT)

  Await.result(bindFuture, 15 seconds)
  Await.result(system.whenTerminated, Duration.Inf)
}