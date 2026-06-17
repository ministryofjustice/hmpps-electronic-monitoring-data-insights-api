package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi

import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.during
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.PopulationBuilder
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.lang.System.getenv
import java.time.Duration

class ExistsInEmdiRealData : Simulation() {

  private val crn: String = getenv("CRN")
  private val stressLevels = listOf(5, 10, 20, 30, 40, 50, 75, 100)
  private val levelDuration: Duration = Duration.ofSeconds(60)

  private fun existsInEmdi(users: Int) = exec(
    http("Exists in EMDI using real data - $users concurrent users - CRN $crn")
      .get("/people/exists/$crn")
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.uri").exists()),
  )

  private fun stressLevel(users: Int): PopulationBuilder = scenario("exists-in-emdi-real-data-$users-users")
    .exec(getToken)
    .during(levelDuration).on(exec(existsInEmdi(users)))
    .injectClosed(constantConcurrentUsers(users).during(levelDuration))

  init {
    val stressSuite = stressLevels
      .map(::stressLevel)
      .reduce { suite, nextLevel -> suite.andThen(nextLevel) }

    setUp(stressSuite)
      .protocols(httpProtocol)
  }
}
