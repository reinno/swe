package swe.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import swe.model.Activity
import swe.service.Task.{ActivityMaster, ActivityPassivePoller}

import scala.concurrent.ExecutionContext

object TaskRoute {
  case class PostTaskClaimingEntity(name: String, version: Option[String] = None, num: Int = 1)
}

class TaskRoute(val apiMaster: ActorRef)(implicit ec: ExecutionContext) extends BaseRoute {
  import TaskRoute._
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
              } ~ {
                askActorRoute[StatusCode](apiMaster,
                  ActivityMaster.PostTaskHeartBeat(runId, ActivityMaster.PostTaskHeartBeat.Entity()))
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
      } ~ {
        path("claiming") {
          post {
            entity(as[PostTaskClaimingEntity]) {
              entity => askActorRoute[ActivityMaster.PollTasks.Response](apiMaster, ActivityPassivePoller.StoreClaiming(
                ActivityMaster.PollTasks.Entity(Activity.Type(entity.name, entity.version), entity.num)
              ))
            }
          }
        }
      } ~ pathEndOrSingleSlash {
        post {
          entity(as[ActivityMaster.PostTaskEntity]) {
            entity => askActorRoute[String](apiMaster, ActivityMaster.PostTask(entity))
          }
        } ~ get {
          parameters('cur_page.as[Int] ? 1, 'per_page_num.as[Int] ? 20) { (cur_page, per_page_num) =>
            askActorRoute[ActivityMaster.GetTasks.Response](apiMaster, ActivityMaster.GetTasks(cur_page, per_page_num))
          }
        }
      }
    }
  }
}
