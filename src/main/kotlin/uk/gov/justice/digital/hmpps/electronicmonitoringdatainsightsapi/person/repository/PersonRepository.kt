package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPeople
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload

interface PersonRepository {
  fun searchPeople(personsQueryCriteria: PeopleQueryCriteria, nextToken: String?): PagedPeople
  fun findByPersonById(personId: String): Person?
  fun findRawCaseloadByDeliusId(deliusId: String): List<RawCaseload>
}
