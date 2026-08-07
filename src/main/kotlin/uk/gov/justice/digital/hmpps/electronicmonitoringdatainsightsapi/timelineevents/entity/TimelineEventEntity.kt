package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "timeline_events")
class TimelineEventEntity(

  @Id
  val id: UUID,

  val occurredAt: Instant,

  val userName: String,

  val crn: String,

  @Enumerated(EnumType.STRING)
  val eventType: EventType,

  val results: Int?,

  val durationMs: Long?,

  @JdbcTypeCode(SqlTypes.JSON)
  val detail: Map<String, Any?> = emptyMap(),
)
