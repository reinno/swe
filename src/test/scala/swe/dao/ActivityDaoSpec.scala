package swe.dao

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import swe.model.Activity
import swe.service.DbService

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ActivityDaoSpec extends FlatSpec with Matchers with BeforeAndAfterAll  {
  import DbService._

  import scala.concurrent.ExecutionContext.Implicits.global

  it should "handle delete task success" in {
    val f  = DbService.isTableExist(Activity.Instance.InstancesDao.tableName)
    f.onComplete {
      case Success(b) =>
        Activity.Instance.InstancesDao.destroy
      case Failure(ex) =>
        println(ex)
    }

    val results = for {
      //result <- {db.run(Activity.Instance.InstancesDao.destroy)}
      result <- db.run(Activity.Instance.InstancesDao.setup())
    } yield result

    val dbFuture = Future(results.recover{case e: java.util.NoSuchElementException => "table exists"})

    dbFuture.onComplete {
      case Success(b) =>
        println(b)
      case Failure(ex) =>
        println(ex)
    }
  }
}
