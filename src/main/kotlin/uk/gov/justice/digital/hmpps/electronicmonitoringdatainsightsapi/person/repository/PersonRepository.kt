package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPersons
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PersonsQueryCriteria

interface PersonRepository {
  fun getPersons(personsQueryCriteria: PersonsQueryCriteria, nextToken: String?): PagedPersons
  fun getPersonById(personId: String): Person?
}
