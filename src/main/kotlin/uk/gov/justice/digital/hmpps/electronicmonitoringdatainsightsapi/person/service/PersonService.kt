package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import org.apache.commons.text.similarity.LevenshteinDistance
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert.PersonMatchAlertService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprPerson
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonSearchRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.entity.PersonMatchScoreEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPeople
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PersonalDetailsSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonMatchScoreRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.round

@Service
class PersonService(
  private val personRepository: PersonRepository,
  private val cprApiClient: CprApiClient,
  private val personMatchScoreRepository: PersonMatchScoreRepository,
  private val personMatchingProperties: PersonMatchingProperties,
  private val personMatchAlertService: PersonMatchAlertService,
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

  fun personMatchScore(cprPerson: CprPerson, emPerson: Person): PersonMatchScoreEntity {
    val crn = cprPerson.identifiers.crns.firstOrNull()
    val personId = emPerson.personId
    val cprNames = nameCandidates(cprPerson.firstName, cprPerson.lastName)
    val cprName = cprNames.firstOrNull()
    val emName = emPerson.personName.normaliseText()
    val cprPostcode = cprPerson.addresses
      .firstOrNull { it.status?.code.equals("M", ignoreCase = true) }
      ?.postcode
      .normalisePostcode()
    val emPostcode = emPerson.zip.normalisePostcode()
    val cprDob = cprPerson.dateOfBirth?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val emDob = emPerson.dob

    val exactNameMatch = cprName != null && cprName == emName
    val exactPostcodeMatch = cprPostcode != null && cprPostcode == emPostcode
    val exactDobMatch = cprDob != null && cprDob == emDob
    val nameScore = cprNames.maxOfOrNull { stringSimilarity(it, emName) } ?: 0.0
    val postcodeScore = stringSimilarity(cprPostcode, emPostcode)
    val dobScore = dateSimilarity(cprDob, emDob)
    val properties = personMatchingProperties
    val totalWeight = properties.nameWeight + properties.postcodeWeight + properties.dateOfBirthWeight
    val overallScore = (
      nameScore * properties.nameWeight +
        postcodeScore * properties.postcodeWeight +
        dobScore * properties.dateOfBirthWeight
      ) / totalWeight

    val match = PersonMatchScoreEntity(
      id = UUID.randomUUID(),
      crn = crn!!,
      personId = personId!!,
      exactNameMatch = exactNameMatch,
      exactPostcodeMatch = exactPostcodeMatch,
      exactDobMatch = exactDobMatch,
      nameScore = percentage(nameScore),
      postcodeScore = percentage(postcodeScore),
      dobScore = percentage(dobScore),
      overallMatchScore = percentage(overallScore),
      createdAt = Instant.now(),
    )
    val previousMatch = personMatchScoreRepository.findFirstByCrnAndPersonIdOrderByCreatedAtDesc(crn, personId)
    val savedMatch = personMatchScoreRepository.save(match)
    if (previousMatch?.overallMatchScore != savedMatch.overallMatchScore) {
      personMatchAlertService.alertIfBelowThreshold(savedMatch)
    }
    return savedMatch
  }

  private fun stringSimilarity(first: String?, second: String?): Double {
    if (first == null || second == null) return 0.0
    if (first == second) return 1.0
    val longestLength = maxOf(first.length, second.length)
    if (longestLength == 0) return 1.0
    val distance = LevenshteinDistance.getDefaultInstance().apply(first, second)
    val similarity = 1.0 - distance.toDouble() / longestLength
    return similarity.takeIf { it >= personMatchingProperties.minimumStringSimilarity } ?: 0.0
  }

  private fun dateSimilarity(first: LocalDate?, second: LocalDate?): Double {
    if (first == null || second == null) return 0.0
    val difference = kotlin.math.abs(ChronoUnit.DAYS.between(first, second))
    return (1.0 - difference.toDouble() / personMatchingProperties.dateOfBirthToleranceDays)
      .coerceIn(0.0, 1.0)
  }

  private fun nameCandidates(firstName: String?, surname: String?): List<String> {
    val normalisedFirstName = firstName.normaliseText()
    val normalisedSurname = surname.normaliseText()
    val fullName = listOfNotNull(normalisedFirstName, normalisedSurname).joinToString(" ")
    val surnameParts = normalisedSurname
      ?.split(Regex("[\\s-]+"))
      ?.filter(String::isNotEmpty)
      .orEmpty()

    return buildList {
      fullName.takeIf(String::isNotEmpty)?.let(::add)
      surnameParts
        .filterNot { it == normalisedSurname }
        .map { listOfNotNull(normalisedFirstName, it).joinToString(" ") }
        .forEach(::add)
    }.distinct()
  }

  private fun String?.normaliseText(): String? = this
    ?.trim()
    ?.lowercase(Locale.UK)
    ?.replace(Regex("\\s+"), " ")
    ?.takeIf(String::isNotEmpty)

  private fun String?.normalisePostcode(): String? = normaliseText()?.replace(" ", "")

  private fun percentage(score: Double): Double = round(score * 10_000) / 100
}
