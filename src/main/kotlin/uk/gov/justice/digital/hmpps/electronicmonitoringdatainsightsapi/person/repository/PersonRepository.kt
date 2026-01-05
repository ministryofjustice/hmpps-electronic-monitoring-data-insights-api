package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person

interface PersonRepository {
  fun findByCrn(crn: String): List<Person>
}