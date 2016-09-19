package swe.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import swe.util.{LogUtil, Constants}

trait BaseRoute {
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
