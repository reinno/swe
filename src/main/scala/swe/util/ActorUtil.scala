package swe.util


import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorRef
import akka.pattern.ask


object ActorUtil {
  def askActor[T](actor: ActorRef, msg: Any)(implicit ex: ExecutionContext): Future[T] = {
    implicit val timeout = Constants.FUTURE_TIMEOUT
    (actor ? msg).asInstanceOf[Future[T]]
  }
}
