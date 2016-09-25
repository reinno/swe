package swe.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{StatusCodes, HttpResponse, HttpRequest}
import akka.stream.Materializer
import org.scalatest.Matchers

import scala.concurrent.Future

object HttpClientServiceHelper {

  trait HttpClientSenderDummy extends HttpClientSender with Matchers {
    import system.dispatcher

    implicit val system: ActorSystem
    implicit val mat: Materializer

    def sendPartial: PartialFunction[HttpRequest, Future[HttpResponse]] = {
      case x =>
        Future(HttpResponse(StatusCodes.InternalServerError))
    }

    def sendHttpReq(req: HttpRequest): Future[HttpResponse] = {
      sendPartial(req)
    }
  }
}
