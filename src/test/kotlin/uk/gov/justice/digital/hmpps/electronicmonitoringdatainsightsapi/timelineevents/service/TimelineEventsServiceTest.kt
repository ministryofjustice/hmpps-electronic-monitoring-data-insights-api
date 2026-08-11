package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventStatistics
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.time.LocalDate

class TimelineEventsServiceTest {
  private val repository = mockk<TimelineEventsRepository>()
  private val service = TimelineEventsService(repository)

  @Test
  fun `returns formatted statistics for the requested day`() {
    val statistics = mockk<TimelineEventStatistics> {
      every { users } returns 222
      every { searches } returns 2991
      every { pops } returns 279
      every { averageDurationMs } returns 5_500.0
      every { maximumDurationMs } returns 59_100
    }
    every {
      repository.getStatistics(
        Instant.parse("2026-06-30T23:00:00Z"),
        Instant.parse("2026-08-11T23:00:00Z"),
      )
    } returns statistics

    val result = service.getStatistics(toDate = LocalDate.parse("2026-08-11"))

    assertThat(result).isEqualTo(
      "222 users, 2991 searches, relating to 279 PoPs. " +
        "Average time to load results 5.5 seconds (Max 59.1 seconds)",
    )
  }

  @Test
  fun `returns zero durations when no searches have durations`() {
    val statistics = mockk<TimelineEventStatistics> {
      every { users } returns 0
      every { searches } returns 0
      every { pops } returns 0
      every { averageDurationMs } returns null
      every { maximumDurationMs } returns null
    }
    every { repository.getStatistics(any(), any()) } returns statistics

    val result = service.getStatistics(
      fromDate = LocalDate.parse("2026-01-01"),
      toDate = LocalDate.parse("2026-01-01"),
    )

    assertThat(result).isEqualTo(
      "0 users, 0 searches, relating to 0 PoPs. " +
        "Average time to load results 0.0 seconds (Max 0.0 seconds)",
    )
    verify {
      repository.getStatistics(
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-02T00:00:00Z"),
      )
    }
  }
}
