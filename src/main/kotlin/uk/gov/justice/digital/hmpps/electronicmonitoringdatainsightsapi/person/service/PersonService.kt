package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPeople
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonRepository

@Service
class PersonService(private val personRepository: PersonRepository) {

  fun searchPeople(personsQueryCriteria: PeopleQueryCriteria, nextToken: String? = null): PagedPeople = this.personRepository.searchPeople(personsQueryCriteria, nextToken)

  fun getPersonById(personId: String): Person? = personRepository.findByPersonById(personId)
}
