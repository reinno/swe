package swe.route

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

trait ApiRoute {
  implicit val actorSystem: ActorSystem
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer

  val apiMaster: ActorRef

  val taskRoute = new TaskRoute(apiMaster)
  val versionRoute = new VersionRoute()

  def route: Route = taskRoute.route ~ versionRoute.route
}

class ApiRouteService(override val apiMaster: ActorRef)
                     (implicit override val actorSystem: ActorSystem, override val mat: Materializer) extends ApiRoute
