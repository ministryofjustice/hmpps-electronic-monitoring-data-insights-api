package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
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
  fun `defaults from date to 1 July 2026`() {
    val today = LocalDate.now(ZoneId.of("Europe/London"))
    every { service.getStatistics(LocalDate.parse("2026-07-01"), today) } returns "statistics"

    assertThat(controller.getStatistics(null, null)).isEqualTo("statistics")
    verify { service.getStatistics(LocalDate.parse("2026-07-01"), today) }
  }
}
