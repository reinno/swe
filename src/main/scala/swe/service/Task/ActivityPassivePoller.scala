package swe.service.Task

import akka.actor.{PoisonPill, ActorRef, Props}
import com.github.nscala_time.time.Imports._
import swe.SettingsActor
import swe.model.Activity
import swe.service.BaseService

object ActivityPassivePoller {
  sealed trait Msg extends BaseService.Msg
  case class StoreClaiming(request: ActivityMaster.PollTasks.Entity) extends Msg
  case class PollSuccess(id: Int, activities: List[Activity.InstanceInput]) extends Msg
  case object Poll

  case class PollTaskClaiming(request: ActivityMaster.PollTasks.Entity, sender: ActorRef, startTime: DateTime)

  def props(apiMaster: ActorRef): Props =
    Props(new ActivityPassivePoller(apiMaster))
}

class ActivityPassivePollerWorker(apiMaster: ActorRef, id: Int, request: ActivityMaster.PollTasks.Entity)
  extends BaseService {

  apiMaster ! ActivityMaster.PollTasks(request)

  override def receive: Receive = {
    case instances: List[Activity.InstanceInput] =>
      context.parent ! ActivityPassivePoller.PollSuccess(id, instances)
      self ! PoisonPill

    case msg =>
      log.error(msg.toString)
  }
}

class ActivityPassivePoller(apiMaster: ActorRef)
  extends BaseService with SettingsActor {
  import ActivityPassivePoller._
  import context.dispatcher

  var id = 0
  var pollTasksRequests: Map[Int, PollTaskClaiming] = Map.empty

  context.system.scheduler.schedule(settings.activityPassivePollInterval, settings.activityPassivePollInterval, self, Poll)

  override def receive: Receive = {
    case StoreClaiming(request) =>
      pollTasksRequests = pollTasksRequests.updated(id, PollTaskClaiming(request, sender(), DateTime.now))
      id += 1

    case PollSuccess(_id, activities) =>
      if (activities.nonEmpty) {
        pollTasksRequests.get(_id) match {
          case Some(claiming) =>
            claiming.sender ! activities
            pollTasksRequests -= _id

          case None =>
        }
      }

    case Poll =>
      pollTasksRequests.foreach{ case (_id, claiming) =>
        val now = DateTime.now
        if (isClaimingTimeout(claiming, now)) {
          claiming.sender ! Nil
        } else {
          context.actorOf(Props(new ActivityPassivePollerWorker(apiMaster, _id, claiming.request)))
        }
      }

    case msg =>
      log.error(msg.toString)
  }

  private def isClaimingTimeout(instance: PollTaskClaiming, now: DateTime): Boolean = {
    (instance.startTime to now).toDuration.getStandardSeconds > settings.activityLongPollTimeout.toSeconds
  }
}
