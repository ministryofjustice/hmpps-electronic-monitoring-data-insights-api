package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import java.time.Instant

data class PositionData(
  val hasPositionData: Boolean,
  val latestPositionGpsDate: Instant?,
)
