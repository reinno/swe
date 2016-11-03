package swe.model

import slick.dbio.{Effect, NoStream, DBIOAction}
import slick.lifted.TableQuery


trait BaseDao {
  val tableName: String
  val query: TableQuery[_]

  def setup(): DBIOAction[_, NoStream, _ <: Effect]
  val destroy: DBIOAction[_, _, _]
}