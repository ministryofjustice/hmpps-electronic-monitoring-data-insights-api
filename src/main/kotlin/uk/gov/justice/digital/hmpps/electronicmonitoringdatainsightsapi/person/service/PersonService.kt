package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonRepository
import kotlin.time.ExperimentalTime

@Service
class PersonService(private val personRepository: PersonRepository) {

  @OptIn(ExperimentalTime::class)
  fun findByCrn(crn: String): List<Person> = personRepository.findByCrn(crn)
}
