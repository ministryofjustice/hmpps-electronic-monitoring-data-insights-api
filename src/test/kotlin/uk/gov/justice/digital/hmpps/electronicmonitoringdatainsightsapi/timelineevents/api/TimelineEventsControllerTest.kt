package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventsStatisticsResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventStatistics
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService.Companion.DEFAULT_FROM_DATE
import java.time.LocalDate
import java.time.ZoneId

class TimelineEventsControllerTest {
  private val service = mockk<TimelineEventsService>()
  private val controller = TimelineEventsController(service)

  @Test
  fun `gets statistics for a specified date range`() {
    val from = LocalDate.parse("2026-07-10")
    val to = LocalDate.parse("2026-08-11")
    every { service.getStatistics(from, to) } returns "statistics"

    assertThat(controller.getStatistics(from, to)).isEqualTo("statistics")
    verify { service.getStatistics(from, to) }
  }

  @Test
  fun `uses the default from date when no date range is supplied`() {
    val today = LocalDate.now(ZoneId.of("Europe/London"))
    every { service.getStatistics(DEFAULT_FROM_DATE, today) } returns "statistics"

    assertThat(controller.getStatistics(null, null)).isEqualTo("statistics")
    verify { service.getStatistics(DEFAULT_FROM_DATE, today) }
  }

  @Test
  fun `gets the statistics summary`() {
    val response = TimelineEventsStatisticsResponse(
      daily = statistics(users = 20, searches = 63, pops = 19),
      weekly = statistics(users = 80, searches = 374, pops = 88),
      monthly = statistics(users = 215, searches = 2643, pops = 263),
      allTime = statistics(users = 222, searches = 2989, pops = 280),
    )
    every { service.getStatisticsSummary() } returns response

    assertThat(controller.getStatisticsSummary()).isEqualTo(response)
    verify { service.getStatisticsSummary() }
  }

  private fun statistics(
    users: Long,
    searches: Long,
    pops: Long,
  ) = mockk<TimelineEventStatistics> {
    every { this@mockk.users } returns users
    every { this@mockk.searches } returns searches
    every { this@mockk.pops } returns pops
  }
}
