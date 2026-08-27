package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "person_match_score")
class PersonMatchScoreEntity(
  @Id
  val id: UUID,
  val crn: String,
  val personId: String,
  val exactNameMatch: Boolean,
  val exactPostcodeMatch: Boolean,
  val exactDobMatch: Boolean,
  val nameScore: Double,
  val postcodeScore: Double,
  val dobScore: Double,
  val overallMatchScore: Double,
  val createdAt: Instant,
  val postcodeMatchedPreviousAddress: Boolean? = null,
)
