package swe.route

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

trait ApiRoute {
  val actorSystem: ActorSystem
  val apiMaster: ActorRef
  val mat: Materializer

  implicit val ec: ExecutionContext = actorSystem.dispatcher

  def route: Route = ???
}

class ApiRouteService(override val apiMaster: ActorRef)
                     (implicit override val actorSystem: ActorSystem, override val mat: Materializer) extends ApiRoute {

}
