package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.lang.System.getenv

class ExistsInEmdiSimulation : Simulation() {

  private val crn: String = "X777777"

  private val existsInEmdi = exec(
    http("Exists in EMDI - CRN $crn")
      .get("/people/exists/$crn")
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.uri").exists()),
  )

  private val smokeTest = scenario("exists-in-emdi-smoke-test")
    .exec(getToken)
    .repeat(20).on(exec(existsInEmdi))

  init {
    setUp(
      smokeTest.injectOpen(atOnceUsers(20)),
    ).protocols(httpProtocol)
  }
}
