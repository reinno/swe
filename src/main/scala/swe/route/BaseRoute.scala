package swe.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import swe.util.{ActorUtil, Constants, LogUtil}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object BaseRoute {
  def askActorRouteOption[T](actor: ActorRef, msg: Any)
                            (implicit exec: ExecutionContext, _marshaller: ToResponseMarshaller[T]): Route = {
    onComplete(ActorUtil.askActor[Option[T]](actor, msg)) {
      case Success(x) =>
        x match {
        case Some(res) => complete(res)
        case None => complete(HttpResponse(StatusCodes.NotFound))
      }

      case Failure(ex) =>
        failWith(ex)
    }
  }

  def askActorRoute[T](actor: ActorRef, msg: Any)
                      (implicit exec: ExecutionContext, _marshaller: ToResponseMarshaller[T]): Route = {
    onComplete(ActorUtil.askActor[T](actor, msg)) {
      case Success(x) =>
        complete(x)

      case Failure(ex) =>
        failWith(ex)
    }
  }
}


trait BaseRoute {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
  implicit val serialization = org.json4s.jackson.Serialization


  protected def doRoute(implicit mat: Materializer): Route
  protected def prefix = Slash ~ "api" / s"${Constants.REST_VERSION}"
  protected val LOG = LogUtil.getLogger(getClass)

  def route: Route = encodeResponse {
    extractMaterializer {implicit mat =>
      rawPathPrefix(prefix) {
        doRoute(mat)
      }
    }
  }
}
