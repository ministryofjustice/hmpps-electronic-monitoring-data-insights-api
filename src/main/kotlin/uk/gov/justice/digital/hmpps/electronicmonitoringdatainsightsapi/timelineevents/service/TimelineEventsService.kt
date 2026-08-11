package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class TimelineEventsService(
  private val timelineEventsRepository: TimelineEventsRepository,
) {

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
}
