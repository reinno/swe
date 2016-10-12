package swe.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import swe.model.Activity
import swe.service.Task.ActivityMaster

import scala.concurrent.ExecutionContext

class TaskRoute(val apiMaster: ActorRef)(implicit ec: ExecutionContext) extends BaseRoute {
  import BaseRoute._
  import Json4sSupport._

  def doRoute(implicit mat: Materializer): Route  = {
    pathPrefix("task") {
      pathPrefix(Segment) {
        runId: String => {
          path("heartbeat") {
            post {
              entity(as[ActivityMaster.PostTaskHeartBeat.Entity]) {
                entity => askActorRoute[StatusCode](apiMaster, ActivityMaster.PostTaskHeartBeat(runId, entity))
              }
            }
          } ~ path("status") {
            post {
              entity(as[ActivityMaster.PostTaskStatus.Entity]) {
                entity => askActorRoute[StatusCode](apiMaster, ActivityMaster.PostTaskStatus(runId, entity))
              }
            }
          } ~ pathEndOrSingleSlash {
            delete {
              askActorRoute[StatusCode](apiMaster, ActivityMaster.DeleteTask(runId))
            } ~ get {
              askActorRouteOption[Activity.Instance](apiMaster, ActivityMaster.GetTask(runId))
            }
          }
        }
      } ~ pathEndOrSingleSlash {
        post {
          entity(as[ActivityMaster.PostTask.Entity]) {
            entity => askActorRoute[String](apiMaster, ActivityMaster.PostTask(entity))
          }
        } ~ get {
          askActorRoute[ActivityMaster.GetTasks.Response](apiMaster, ActivityMaster.GetTasks)
        }
      }
    }
  }
}
