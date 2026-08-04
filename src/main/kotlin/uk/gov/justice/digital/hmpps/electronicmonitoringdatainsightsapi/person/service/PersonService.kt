package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonSearchRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPeople
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PersonalDetailsSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonRepository
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class PersonService(
  private val personRepository: PersonRepository,
  private val cprApiClient: CprApiClient,
) {

  fun searchPeople(personsQueryCriteria: PeopleQueryCriteria, nextToken: String? = null): PagedPeople = this.personRepository.searchPeople(personsQueryCriteria, nextToken)

  fun getPersonById(personId: String): Person? = personRepository.findByPersonById(personId)

  fun getRawCaseloadByDeliusId(deliusId: String): List<RawCaseload> = personRepository.findRawCaseloadByDeliusId(deliusId)

  fun searchPeopleByPersonalDetails(request: PersonSearchRequest): List<Person> = personRepository.findByPersonalDetails(resolveSearchCriteria(request))
    .distinctBy(Person::personId)
    .updateOutsidePeriodFlag()

  private fun List<Person>.updateOutsidePeriodFlag(): List<Person> {
    val today = LocalDate.now()

    for (person in this) {
      val startDate = person.orderStartDate?.atZone(ZoneOffset.UTC)?.toLocalDate() ?: continue
      val endDate = person.orderEndDate?.atZone(ZoneOffset.UTC)?.toLocalDate() ?: continue
      person.outsideOrderPeriod = today.isBefore(startDate) || today.isAfter(endDate)
    }

    return this
  }

  private fun resolveSearchCriteria(request: PersonSearchRequest): PersonalDetailsSearchCriteria {
    val crn = request.crn?.trim()?.takeIf(String::isNotEmpty)
    if (crn != null) {
      val cprPerson = cprApiClient.getPersonByCrn(crn)
      return PersonalDetailsSearchCriteria(
        forename = requireNotNull(cprPerson.firstName?.takeIf(String::isNotBlank)) {
          "CPR person $crn does not have a forename"
        },
        surname = requireNotNull(cprPerson.lastName?.takeIf(String::isNotBlank)) {
          "CPR person $crn does not have a surname"
        },
        dateOfBirth = if (request.searchByNameOnly) {
          null
        } else {
          cprPerson.dateOfBirth?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        },
        postcode = if (request.searchByNameOnly) {
          null
        } else {
          cprPerson.addresses
            .firstOrNull { it.status?.code.equals("M", ignoreCase = true) }
            ?.postcode
            ?.takeIf(String::isNotBlank)
        },
      )
    }

    return PersonalDetailsSearchCriteria(
      forename = requireNotNull(request.forename),
      surname = requireNotNull(request.surname),
      dateOfBirth = request.dateOfBirth,
      postcode = request.postcode?.takeIf(String::isNotBlank),
    )
  }
}
