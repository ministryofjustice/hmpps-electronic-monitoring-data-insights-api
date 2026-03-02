package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPersons
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonRepository
import kotlin.time.ExperimentalTime

@Service
class PersonService(private val personRepository: PersonRepository) {

  fun getPersons(personsQueryCriteria: PersonsQueryCriteria, nextToken: String?): PagedPersons = this.personRepository.getPersons(personsQueryCriteria, nextToken)

  @OptIn(ExperimentalTime::class)
  fun getPersonById(personId: String): Person? = personRepository.getPersonById(personId)
}
