package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.ServiceProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.RegionalAdoptionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.RegionalAdoption
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventStatistics
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class TimelineEventsServiceTest {
  private val repository = mockk<TimelineEventsRepository>()
  private val properties = ServiceProperties("https://api.test", "https://ui.test", listOf(" London ", "", "London"))
  private val service = TimelineEventsService(repository, properties)

  @Test
  fun `returns zero regional total without querying adoption when no areas are configured`() {
    val unconfiguredService = TimelineEventsService(repository, properties.copy(deliusResponsibleOrganisations = listOf(" ")))
    every { repository.getStatistics(any(), any()) } returns statistics(users = 2, searches = 5, pops = 1)

    val response = unconfiguredService.getMonthlyMetrics(YearMonth.of(2026, 9))

    assertThat(response.regions).containsExactly(RegionalAdoptionResponse("TOTAL", 0))
    verify(exactly = 0) { repository.getAdoption(any(), any(), any()) }
  }

  @Test
  fun `uses London calendar month boundaries including daylight saving and year changes`() {
    val cases = listOf(
      Triple("2026-09", "2026-08-31T23:00:00Z", "2026-09-30T23:00:00Z"),
      Triple("2026-10", "2026-09-30T23:00:00Z", "2026-11-01T00:00:00Z"),
      Triple("2026-12", "2026-12-01T00:00:00Z", "2027-01-01T00:00:00Z"),
      Triple("2028-02", "2028-02-01T00:00:00Z", "2028-03-01T00:00:00Z"),
    )
    val row = mockk<RegionalAdoption> {
      every { region } returns "TOTAL"
      every { adoption } returns 3L
    }
    cases.forEach { (month, from, to) ->
      every { repository.getAdoption(Instant.parse(from), Instant.parse(to), listOf("London")) } returns listOf(row)

      every { repository.getStatistics(Instant.parse(from), Instant.parse(to)) } returns statistics(users = 2, searches = 5, pops = 1)

      val response = service.getMonthlyMetrics(YearMonth.parse(month))

      assertThat(response.statistics.users).isEqualTo(2)
      assertThat(response.statistics.searches).isEqualTo(5)
      assertThat(response.statistics.pops).isEqualTo(1)
      verify { repository.getStatistics(Instant.parse(from), Instant.parse(to)) }
      assertThat(response.month).isEqualTo(month)
      assertThat(response.regions).containsExactly(RegionalAdoptionResponse("TOTAL", 3L))
      verify { repository.getAdoption(Instant.parse(from), Instant.parse(to), listOf("London")) }
    }
  }

  @Test
  fun `returns statistics for completed daily weekly monthly and all-time periods`() {
    every {
      repository.getStatistics(
        Instant.parse("2026-08-09T23:00:00Z"),
        Instant.parse("2026-08-10T23:00:00Z"),
      )
    } returns statistics(
      users = 20,
      searches = 63,
      pops = 19,
      averageLoadDurationMs = 5_500.0,
      averagePersonLoadDurationMs = 4_000.0,
      averageLocationLoadDurationMs = 7_000.0,
    )
    every {
      repository.getStatistics(
        Instant.parse("2026-08-03T23:00:00Z"),
        Instant.parse("2026-08-10T23:00:00Z"),
      )
    } returns statistics(users = 80, searches = 374, pops = 88)
    every {
      repository.getStatistics(
        Instant.parse("2026-07-11T23:00:00Z"),
        Instant.parse("2026-08-10T23:00:00Z"),
      )
    } returns statistics(users = 215, searches = 2643, pops = 263)
    every {
      repository.getStatistics(
        Instant.EPOCH,
        Instant.parse("2026-08-10T23:00:00Z"),
      )
    } returns statistics(users = 222, searches = 2989, pops = 280)

    val result = service.getStatisticsSummary(LocalDate.parse("2026-08-11"))

    assertThat(result.daily.users).isEqualTo(20)
    assertThat(result.daily.searches).isEqualTo(63)
    assertThat(result.daily.pops).isEqualTo(19)
    assertThat(result.daily.averageLoadDurationMs).isEqualTo(5_500.0)
    assertThat(result.daily.averagePersonLoadDurationMs).isEqualTo(4_000.0)
    assertThat(result.daily.averageLocationLoadDurationMs).isEqualTo(7_000.0)
    assertThat(result.weekly.users).isEqualTo(80)
    assertThat(result.weekly.searches).isEqualTo(374)
    assertThat(result.weekly.pops).isEqualTo(88)
    assertThat(result.monthly.users).isEqualTo(215)
    assertThat(result.monthly.searches).isEqualTo(2643)
    assertThat(result.monthly.pops).isEqualTo(263)
    assertThat(result.allTime.users).isEqualTo(222)
    assertThat(result.allTime.searches).isEqualTo(2989)
    assertThat(result.allTime.pops).isEqualTo(280)
  }

  @Test
  fun `returns formatted statistics for the requested day`() {
    val statistics = mockk<TimelineEventStatistics> {
      every { users } returns 222
      every { searches } returns 2991
      every { pops } returns 279
      every { averageLoadDurationMs } returns 5_500.0
      every { averagePersonLoadDurationMs } returns 4_000.0
      every { averageLocationLoadDurationMs } returns 7_000.0
      every { maximumDurationMs } returns 59_100
      every { averageTimeSpentSeconds } returns 245.6
    }
    every {
      repository.getStatistics(
        Instant.parse("2026-07-07T23:00:00Z"),
        Instant.parse("2026-08-11T23:00:00Z"),
      )
    } returns statistics

    val result = service.getStatistics(toDate = LocalDate.parse("2026-08-11"))

    assertThat(result).isEqualTo(
      "222 users, 2991 searches, relating to 279 PoPs. " +
        "Average time to load results 5.5 seconds " +
        "(Person 4.0 seconds, Location 7.0 seconds, Max 59.1 seconds). " +
        "Average time spent on page 245.6 seconds",
    )
  }

  @Test
  fun `returns zero durations when no searches have durations`() {
    val statistics = mockk<TimelineEventStatistics> {
      every { users } returns 0
      every { searches } returns 0
      every { pops } returns 0
      every { averageLoadDurationMs } returns null
      every { averagePersonLoadDurationMs } returns null
      every { averageLocationLoadDurationMs } returns null
      every { maximumDurationMs } returns null
      every { averageTimeSpentSeconds } returns null
    }
    every { repository.getStatistics(any(), any()) } returns statistics

    val result = service.getStatistics(
      fromDate = LocalDate.parse("2026-01-01"),
      toDate = LocalDate.parse("2026-01-01"),
    )

    assertThat(result).isEqualTo(
      "0 users, 0 searches, relating to 0 PoPs. " +
        "Average time to load results 0.0 seconds " +
        "(Person 0.0 seconds, Location 0.0 seconds, Max 0.0 seconds). " +
        "Average time spent on page 0.0 seconds",
    )
    verify {
      repository.getStatistics(
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-02T00:00:00Z"),
      )
    }
  }

  private fun statistics(
    users: Long,
    searches: Long,
    pops: Long,
    averageLoadDurationMs: Double? = null,
    averagePersonLoadDurationMs: Double? = null,
    averageLocationLoadDurationMs: Double? = null,
  ) = mockk<TimelineEventStatistics> {
    every { this@mockk.users } returns users
    every { this@mockk.searches } returns searches
    every { this@mockk.pops } returns pops
    every { this@mockk.averageLoadDurationMs } returns averageLoadDurationMs
    every { this@mockk.averagePersonLoadDurationMs } returns averagePersonLoadDurationMs
    every { this@mockk.averageLocationLoadDurationMs } returns averageLocationLoadDurationMs
    every { this@mockk.maximumDurationMs } returns null
    every { this@mockk.averageTimeSpentSeconds } returns null
  }
}
