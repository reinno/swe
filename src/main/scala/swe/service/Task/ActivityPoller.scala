package swe.service.Task

import akka.actor.{ActorRef, Props}
import akka.stream.Materializer
import swe.model.Activity
import swe.service.{BaseService, HttpClientService}

object ActivityPoller {
  sealed trait Msg extends BaseService.Msg

  case class NewTaskNotify(activityType: Activity.Type) extends Msg with BaseService.Notify

  def props(apiMaster: ActorRef)(implicit httpClientFactory: HttpClientService.HttpClientFactory,  mat: Materializer): Props =
    Props(new ActivityPoller(apiMaster))
}

class ActivityPoller(apiMaster: ActorRef)(implicit httpClientFactory: HttpClientService.HttpClientFactory, mat: Materializer) extends BaseService {
  import ActivityPoller._

  //start timer

  override def receive: Receive = {
    case msg: NewTaskNotify =>
      // find task service
      // poll task
      apiMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(msg.activityType, 1))

    case msg: ActivityMaster.PollTasks.Response =>


    case _ =>
  }
}
