package swe.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import swe.model.Activity
import swe.service.Task.TaskMaster

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
              entity(as[TaskMaster.PostTaskHeartBeat.Entity]) {
                entity => askActorRoute[StatusCode](apiMaster, TaskMaster.PostTaskHeartBeat(runId, entity))
              }
            }
          } ~ path("status") {
            post {
              entity(as[TaskMaster.PostTaskStatus.Entity]) {
                entity => askActorRoute[StatusCode](apiMaster, TaskMaster.PostTaskStatus(runId, entity))
              }
            }
          } ~ pathEndOrSingleSlash {
            delete {
              askActorRoute[StatusCode](apiMaster, TaskMaster.DeleteTask(runId))
            } ~ get {
              askActorRouteOption[Activity.Instance](apiMaster, TaskMaster.GetTask(runId))
            }
          }
        }
      } ~ pathEndOrSingleSlash {
        post {
          entity(as[TaskMaster.PostTask.Entity]) {
            entity => askActorRoute[String](apiMaster, TaskMaster.PostTask(entity))
          }
        } ~ get {
          askActorRoute[TaskMaster.GetTasks.Response](apiMaster, TaskMaster.GetTasks)
        }
      }
    }
  }
}
