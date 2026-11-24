package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.entity.LatestGpsPingEntity
import java.time.Instant

@Repository
interface LatestGpsPingRepository : JpaRepository<LatestGpsPingEntity, String> {
  @Query("SELECT ping.gpsDate FROM LatestGpsPingEntity ping WHERE ping.crn = :crn")
  fun findGpsDate(@Param("crn") crn: String): Instant?

  @Transactional
  @Modifying
  @Query(
    value = """
      INSERT INTO latest_gps_ping (crn, person_id, gps_date)
      VALUES (:crn, :personId, :gpsDate)
      ON CONFLICT (crn) DO UPDATE
      SET person_id = EXCLUDED.person_id, gps_date = EXCLUDED.gps_date
      WHERE EXCLUDED.gps_date > latest_gps_ping.gps_date
    """,
    nativeQuery = true,
  )
  fun upsert(
    @Param("crn") crn: String,
    @Param("personId") personId: String?,
    @Param("gpsDate") gpsDate: Instant,
  )
}
