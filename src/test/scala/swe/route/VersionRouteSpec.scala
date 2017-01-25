package swe.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class VersionRouteSpec extends FlatSpec with ScalatestRouteTest with Matchers with BeforeAndAfterAll {
  val route = new VersionRoute().route

  it should "handle get version success" in {
    Get(s"/api/v1/version") ~> route ~> check {
      print(response)
      status shouldBe StatusCodes.OK
    }
  }
}
