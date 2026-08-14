package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.entity.PersonMatchScoreEntity
import java.util.UUID

@Repository
interface PersonMatchScoreRepository : JpaRepository<PersonMatchScoreEntity, UUID>
