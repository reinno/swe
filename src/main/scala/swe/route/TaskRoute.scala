package swe.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import swe.interface.TaskInterface
import swe.service.Task.TaskMaster

import scala.concurrent.ExecutionContext

class TaskRoute(val apiMaster: ActorRef)(implicit ec: ExecutionContext) extends BaseRoute {
  import BaseRoute._
  import Json4sSupport._

  def doRoute(implicit mat: Materializer): Route  = {
    pathPrefix("task") {
      pathPrefix(Segment) {
        taskId: String => {
          path("heartbeat") {
            post {
              entity(as[TaskInterface.PostTaskHeartBeat]) {
                msg => askActorRoute[StatusCode](apiMaster, TaskMaster.PostTaskHeartBeat(taskId, msg))
              }
            }
          } ~ path("status") {
            post {
              entity(as[TaskInterface.PostTaskStatus]) {
                msg => askActorRoute[StatusCode](apiMaster, TaskMaster.PostTaskStatus(taskId, msg))
              }
            }
          } ~ pathEndOrSingleSlash {
            delete {
              askActorRoute[StatusCode](apiMaster, TaskMaster.DeleteTask(taskId))
            }
          }
        }
      } ~ pathEndOrSingleSlash {
        post {
          entity(as[TaskInterface.PostTask]) {
            msg => askActorRoute[String](apiMaster, TaskMaster.PostTask(msg))
          }
        }
      }
    }
  }
}
