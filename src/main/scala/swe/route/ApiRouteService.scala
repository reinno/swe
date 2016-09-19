package swe.route

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext

trait ApiRoute {
  val actorSystem: ActorSystem
  val apiMaster: ActorRef

  implicit val ec: ExecutionContext = actorSystem.dispatcher

  def route: Route = ???
}

class ApiRouteService(override val actorSystem: ActorSystem,
                      override val apiMaster: ActorRef) extends ApiRoute {

}
