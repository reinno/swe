package swe.model

import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import com.github.nscala_time.time.Imports.DateTime
//import slick.driver.H2Driver.api._
import slick.driver.MySQLDriver.api._

object Activity {

  case class Type(name: String, version: Option[String] = None)

  case class Configuration(description: String,
                           defaultTaskList: String,
                           defaultTaskScheduleToStart: Option[Duration] = Some(Duration.Inf),
                           defaultTaskScheduleToClose: Option[Duration] = Some(Duration.Inf),
                           defaultTaskPriority: Int = 0,
                           defaultTaskHeartbeatTimeout: Option[Duration] = Some(Duration.Inf),
                           defaultTaskStartToCloseTimeout: Option[Duration] = Some(Duration.Inf))


  object InstanceInput {
    def apply(instance: Instance): InstanceInput ={
      InstanceInput(instance.activityId, instance.runId,
        instance.activityType, instance.workflowExecution, instance.startedEventId, instance.input)
    }
  }
  case class InstanceInput(activityId: Option[String],
                           runId: String,
                           activityType: Activity.Type,
                           workflowExecution: Option[Workflow.Instance] = None,
                           startedEventId: Option[String],
                           input: Option[String] = None)


  case class InstanceInfo(execution: Option[Workflow.Instance] = None,
                          activityType: Activity.Type,
                          startTimeStamp: DateTime,
                          lastHeartBeatTimeStamp: DateTime,
                          closeTimeStamp: DateTime,
                          currentStatus: String,
                          closeStatus: Option[String],
                          cancelRequested: Boolean = false)

  sealed abstract class Status(val value: String) {
    val isEndedStatus: Boolean = false
  }

  object Status {

    case object WaitScheduled extends Status("WaitScheduled")

    case object Initialize extends Status("Initialize")

    case object Running extends Status("Running")

    case object Deleted extends Status("Deleted") {
      override val isEndedStatus: Boolean = true
    }

    case object Failed extends Status("Failed") {
      override val isEndedStatus: Boolean = true
    }

    case object Complete extends Status("Complete") {
      override val isEndedStatus: Boolean = true
    }

    case object Timeout extends Status("Timeout") {
      override val isEndedStatus: Boolean = true
    }

    def unapply(s: String): Option[Status] =
      s match {
        case "WaitScheduled" => Some(WaitScheduled)
        case "Initialize" => Some(Initialize)
        case "Running" => Some(Running)
        case "Deleted" => Some(Deleted)
        case "Failed" => Some(Failed)
        case "Complete" => Some(Complete)
        case "Timeout" => Some(Timeout)
        case _ => None
      }
  }

  case class Event(timestamp: DateTime, status: String, details: Option[String])

  case class Instance(activityId: Option[String] = None,
                      runId: String,
                      activityType: Activity.Type,
                      workflowExecution: Option[Workflow.Instance] = None,
                      startedEventId: Option[String] = None,
                      scheduleToStart: Option[Duration] = Some(Duration.Inf),
                      scheduleToClose: Option[Duration] = Some(Duration.Inf),
                      priority: Int = 0,
                      heartbeatTimeout: Duration = Duration.Inf,
                      startToCloseTimeout: Option[Duration] = Some(Duration.Inf),
                      history: List[Event] = Nil,
                      input: Option[String] = None,
                      output: Option[String] = None,
                      createTimeStamp: DateTime,
                      startTimeStamp: Option[DateTime] = None,
                      lastHeartBeatTimeStamp: Option[DateTime] = None,
                      closeTimeStamp: Option[DateTime] = None,
                      currentStatus: String,
                      closeStatus: Option[String] = None,
                      cancelRequested: Boolean = false)
  object Instance {
    def apply(plainData: PlainData): Instance = {
      Instance(plainData.activityId, plainData.runId, Activity.Type(plainData.activityName, plainData.activityVersion),
        None, None, None, None, plainData.priority,
        Duration(plainData.heartbeatTimeoutSecs, SECONDS), plainData.startToCloseTimeoutSecs.map(Duration(_, SECONDS)),
        Nil, plainData.input, plainData.output,
        DateTime.parse(plainData.createTimeStamp), plainData.startTimeStamp.map(DateTime.parse),
        plainData.lastHeartBeatTimeStamp.map(DateTime.parse), plainData.closeTimeStamp.map(DateTime.parse),
        plainData.currentStatus, plainData.closeStatus, plainData.cancelRequested)
    }

    case class PlainData(activityId: Option[String] = None, runId: String,
                         activityName: String, activityVersion: Option[String] = None,
                         priority: Int,
                         heartbeatTimeoutSecs: Int, startToCloseTimeoutSecs: Option[Int],
                         input: Option[String] = None, output: Option[String] = None,
                         createTimeStamp: String, startTimeStamp: Option[String] = None,
                         lastHeartBeatTimeStamp: Option[String] = None, closeTimeStamp: Option[String] = None,
                         currentStatus: String, closeStatus: Option[String] = None, cancelRequested: Boolean) {
      def apply(instance: Instance): PlainData = {
        PlainData(instance.activityId, instance.runId,
          instance.activityType.name, instance.activityType.version,
          instance.priority,
          instance.heartbeatTimeout.toSeconds.toInt, instance.startToCloseTimeout.map(_.toSeconds.toInt),
          instance.input, instance.output,
          instance.createTimeStamp.toString(), instance.startTimeStamp.map(_.toString()),
          instance.lastHeartBeatTimeStamp.map(_.toString()), instance.closeTimeStamp.map(_.toString()),
          instance.currentStatus, instance.closeStatus, instance.cancelRequested)
      }
    }

    object InstancesDao extends BaseDao {
      val tableName: String = "ACTIVITY_INSTANCES"

      class Instances(tag: Tag) extends Table[PlainData](tag, "ACTIVITY_INSTANCES") {
        def activityId = column[Option[String]]("ACTIVITY_ID")
        def runId = column[String]("RUN_ID", O.PrimaryKey)
        def activityName = column[String]("NAME")
        def activityVersion = column[Option[String]]("VERSION")
        def priority = column[Int]("PRIORITY")
        def heartbeatTimeoutSec = column[Int]("HEARTBEAT_TIMEOUT_SECS")
        def startToCloseTimeoutSec = column[Option[Int]]("START_TO_CLOSE_TIMEOUT_SECS")
        def input = column[Option[String]]("INPUT")
        def output = column[Option[String]]("OUTPUT")
        def createTimeStamp = column[String]("CREATE_TIMESTAMP")
        def startTimeStamp = column[Option[String]]("START_TIMESTAMP")
        def lastHeartBeatTimeStamp = column[Option[String]]("LAST_HEARTBEAT_TIMESTAMP")
        def closeTimeStamp = column[Option[String]]("CLOSE_TIMESTAMP")
        def currentStatus = column[String]("CURRENT_STATUS")
        def closeStatus = column[Option[String]]("CLOSE_STATUS")
        def cancelRequested = column[Boolean]("CANCEL_REQUESTED")

        // Every table needs a * projection with the same type as the table's type parameter
        def * = (activityId, runId, activityName, activityVersion, priority,
          heartbeatTimeoutSec, startToCloseTimeoutSec, input, output,
          createTimeStamp, startTimeStamp, lastHeartBeatTimeStamp, closeTimeStamp,
          currentStatus, closeStatus, cancelRequested) <> (PlainData.tupled, PlainData.unapply)
      }

      val query = TableQuery[Instances]

      val findByRunId = query.findBy(_.runId)
      //val findAll = query.

      def setup() = DBIO.seq (
        query.schema.create,
        query += PlainData(Some("222"), "0", "demo", None, 0, 10, None, None, None, "1984", None, None, None, "Running", None, false)
      )

      val destroy = DBIO.seq (
        query.schema.drop
      )
    }
  }



}
