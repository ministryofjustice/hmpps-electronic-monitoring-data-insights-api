package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi

import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.during
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.feed
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.listFeeder
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.PopulationBuilder
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.lang.System.getenv
import java.time.Duration

class PositionsRealData : Simulation() {

  private val people = getenv("POSITIONS_PEOPLE")?.let { configuredPeople ->
    configuredPeople.split(",").map { entry ->
      val pair = entry.split("/").map(String::trim)
      require(pair.size == 2 && pair.all { it.isNotBlank() }) {
        "POSITIONS_PEOPLE must be a comma-separated list of CRN/personId pairs"
      }
      mapOf("crn" to pair[0], "personId" to pair[1])
    }
  } ?: listOf(
    mapOf(
      "crn" to requireNotNull(getenv("CRN")?.takeIf { it.isNotBlank() }) { "CRN or POSITIONS_PEOPLE must be set" },
      "personId" to (getenv("PERSON_ID") ?: "10001"),
    ),
  )
  private val peopleFeeder = listFeeder(people).circular()
  private val fromDate: String = getenv("POSITIONS_FROM") ?: "2026-04-01T00:00:00Z"
  private val toDate: String = getenv("POSITIONS_TO") ?: "2026-04-02T00:00:00Z"
  private val stressLevels = listOf(5, 10, 20, 30, 40, 50, 75, 100)
  private val levelDuration: Duration = Duration.ofSeconds(60)

  private fun positions(users: Int) = exec(
    http("Positions using real data - $users concurrent users")
      .get("/people/#{personId}/locations")
      .queryParam("crn", "#{crn}")
      .queryParam("from", fromDate)
      .queryParam("to", toDate)
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.locations").exists()),
  )

  private fun stressLevel(users: Int): PopulationBuilder = scenario("positions-real-data-$users-users")
    .exec(getToken)
    .during(levelDuration).on(feed(peopleFeeder).exec(positions(users)))
    .injectClosed(constantConcurrentUsers(users).during(levelDuration))

  init {
    val stressSuite = stressLevels
      .map(::stressLevel)
      .reduce { suite, nextLevel -> suite.andThen(nextLevel) }

    setUp(stressSuite)
      .protocols(httpProtocol)
  }
}
