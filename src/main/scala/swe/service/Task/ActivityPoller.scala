package swe.service.Task

import akka.actor.{ActorRef, Props}
import akka.stream.Materializer
import swe.SettingsActor
import swe.model.Activity
import swe.service.{BaseService, HttpClientService}

import scala.collection.mutable

object ActivityPoller {
  sealed trait Msg extends BaseService.Msg

  case class NewTaskNotify(activityType: Activity.Type) extends Msg with BaseService.Notify

  def props(apiMaster: ActorRef)(implicit httpClientFactory: HttpClientService.HttpClientFactory,  mat: Materializer): Props =
    Props(new ActivityPoller(apiMaster))
}

class ActivityPoller(apiMaster: ActorRef)(implicit httpClientFactory: HttpClientService.HttpClientFactory, mat: Materializer)
  extends BaseService with SettingsActor {
  import ActivityPoller._
  import context.dispatcher

  //start timer

  val pollTaskQueue: mutable.Queue[Activity.Type] = mutable.Queue.empty

  context.system.scheduler.schedule(settings.activityPollInterval, settings.activityPollInterval, self, "poll")

  override def receive: Receive = {
    case msg: NewTaskNotify =>
      pollTaskQueue.enqueue(msg.activityType)

    case msg: ActivityMaster.PollTasks.Response =>

    case "poll" =>
      pollTaskQueue.dequeueFirst(_ => true).foreach{
        activityType =>
          apiMaster ! ActivityMaster.PollTasks(ActivityMaster.PollTasks.Entity(activityType, 1))
      }

    case _ =>
  }
}
