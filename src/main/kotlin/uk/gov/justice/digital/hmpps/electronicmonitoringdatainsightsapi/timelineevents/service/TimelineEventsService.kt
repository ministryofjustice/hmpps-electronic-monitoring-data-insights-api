package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventsStatisticsResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class TimelineEventsService(
  private val timelineEventsRepository: TimelineEventsRepository,
) {

  fun getStatisticsSummary(
    today: LocalDate = LocalDate.now(REPORTING_TIME_ZONE),
  ): TimelineEventsStatisticsResponse {
    val to = today.atStartOfDay(REPORTING_TIME_ZONE).toInstant()

    fun statisticsFrom(from: Instant) = timelineEventsRepository
      .getStatistics(from, to)

    fun statisticsFrom(from: LocalDate) = statisticsFrom(
      from.atStartOfDay(REPORTING_TIME_ZONE).toInstant(),
    )

    return TimelineEventsStatisticsResponse(
      daily = statisticsFrom(today.minusDays(1)),
      weekly = statisticsFrom(today.minusDays(7)),
      monthly = statisticsFrom(today.minusDays(30)),
      allTime = statisticsFrom(Instant.EPOCH),
    )
  }

  fun getStatistics(
    fromDate: LocalDate = DEFAULT_FROM_DATE,
    toDate: LocalDate = LocalDate.now(REPORTING_TIME_ZONE),
  ): String {
    require(!fromDate.isAfter(toDate)) {
      "from date must not be after to date"
    }

    val from = fromDate.atStartOfDay(REPORTING_TIME_ZONE).toInstant()
    val to = toDate.plusDays(1).atStartOfDay(REPORTING_TIME_ZONE).toInstant()
    val statistics = timelineEventsRepository.getStatistics(from, to)

    val averageSeconds = (statistics.averageDurationMs ?: 0.0) / MILLIS_PER_SECOND
    val maximumSeconds = (statistics.maximumDurationMs ?: 0L).toDouble() / MILLIS_PER_SECOND

    return String.format(
      Locale.UK,
      "%d users, %d searches, relating to %d PoPs. Average time to load results %.1f seconds (Max %.1f seconds)",
      statistics.users,
      statistics.searches,
      statistics.pops,
      averageSeconds,
      maximumSeconds,
    )
  }

  fun record(
    startedAt: Long,
    userName: String,
    crn: String?,
    eventType: EventType,
    results: Int?,
    detail: Map<String, Any?> = emptyMap(),
  ) {
    require(userName.isNotBlank()) {
      "userName must not be blank"
    }

    val durationMs = TimeUnit.NANOSECONDS.toMillis(
      System.nanoTime() - startedAt,
    )

    val event = TimelineEventEntity(
      id = UUID.randomUUID(),
      occurredAt = Instant.now(),
      userName = userName,
      crn = crn ?: "UNKNOWN",
      eventType = eventType,
      results = results,
      durationMs = durationMs,
      detail = detail,
    )
    try {
      timelineEventsRepository.save(event)
    } catch (exception: Exception) {
      log.error(exception) {
        "Failed to persist timeline event for activityCode=$eventType, userName=$userName"
      }
    }
  }

  companion object {
    val REPORTING_TIME_ZONE: ZoneId = ZoneId.of("Europe/London")
    val DEFAULT_FROM_DATE: LocalDate = LocalDate.of(2026, 7, 8) // this is the date the service went into prod trial testing
    const val MILLIS_PER_SECOND = 1000.0
  }
}
