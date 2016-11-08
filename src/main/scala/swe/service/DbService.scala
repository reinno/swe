package swe.service

//import slick.driver.H2Driver.api._
import slick.backend.DatabaseConfig
import slick.driver.MySQLDriver.api._
import slick.driver.MySQLDriver
import slick.jdbc.meta.MTable
import swe.model.BaseDao

import scala.concurrent.Future


object DbService {
  import scala.concurrent.ExecutionContext.Implicits.global

  //val db = Database.forConfig("postgres")
  //val dbConfig: DatabaseConfig[PostgresDriver] = DatabaseConfig.forConfig("h2mem1")
  //val db = Database.forConfig("db.default")
  val dbConfig: DatabaseConfig[MySQLDriver] = DatabaseConfig.forConfig("db.default")
  val db = dbConfig.db

  def isTableExist(tableName: String): Future[Boolean] = {
    db.run(MTable.getTables(tableName)).map(_.nonEmpty)
  }

  def init(tableList: List[_ <: BaseDao]): Future[List[_]] = {
    val results = tableList.map(table =>
      for {
        exist <- isTableExist(table.tableName)
        if !exist
        result <- db.run(table.setup())
      } yield result)

    Future.sequence(results.map(_.recover{case e: java.util.NoSuchElementException => "table exists"}))
  }
}
