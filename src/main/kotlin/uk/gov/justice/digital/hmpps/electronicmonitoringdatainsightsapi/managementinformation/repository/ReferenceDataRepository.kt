package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import java.util.UUID

interface ReferenceDataRepository : Repository<TimelineEventEntity, UUID> {
  @Query(
    """
        SELECT DISTINCT TRIM(pdus.value) AS pdu
        FROM timeline_events e
        CROSS JOIN LATERAL jsonb_array_elements_text(NULLIF(e.crn_pdus, CAST('null' AS jsonb))) AS pdus(value)
        WHERE NULLIF(TRIM(pdus.value), '') IS NOT NULL
        ORDER BY pdu
    """,
    nativeQuery = true,
  )
  fun getPduList(): List<String>

  @Query(
    """
        SELECT DISTINCT TRIM(areas.value) AS region
        FROM timeline_events e
        CROSS JOIN LATERAL unnest(string_to_array(e.crn_probation_areas, ',')) AS areas(value)
        WHERE NULLIF(TRIM(areas.value), '') IS NOT NULL
        ORDER BY region
    """,
    nativeQuery = true,
  )
  fun getRegionList(): List<String>
}
