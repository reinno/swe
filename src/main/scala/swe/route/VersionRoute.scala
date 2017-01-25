package swe.route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import swe.version.BuildInfo


class VersionRoute extends BaseRoute {
  import Json4sSupport._

  def doRoute(implicit mat: Materializer): Route  = {
    path("version") {
      get {
        complete(BuildInfo.toString)
      }
    }
  }
}
