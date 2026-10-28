package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import org.apache.commons.text.similarity.LevenshteinDistance
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert.PersonMatchAlertService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprPerson
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.PersonNoPositionsException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.PersonNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.PersonOutsideOrderPeriodException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.PersonWrongEnforceableConditionException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.PersonWrongRegionException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.jpa.Constants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.ComparedValue
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.EmCompareResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonSearchRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PostcodeComparedValue
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
  awsProperties: AwsProperties,
) {

  private val responsibleOrganisations = awsProperties.athena.responsibleOrganisations
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toSet()

  fun searchPeople(personsQueryCriteria: PeopleQueryCriteria, nextToken: String? = null): PagedPeople {
    val latestPeople = latestRecords(fetchAllPeople(personsQueryCriteria, nextToken))
    // if no person found at all -> return.
    if (latestPeople.isEmpty()) throw PersonNotFoundException()
    // filter on enforceable condition
    // filter the list on enforceable condition = location_monitoring if none are found then return 404 with message.
    val locationMonitoringPeople = filterByEnforceableCondition(latestPeople)
    if (locationMonitoringPeople.isEmpty()) {
      val enforceableConditions = latestPeople.map { it.enforceableCondition?.takeIf(String::isNotBlank) ?: "Unknown" }.distinct()
      throw PersonWrongEnforceableConditionException(enforceableConditions.joinToString(", "))
    }

    // filter on responsible organisation
    val responsibleOrgPeople = filterByResponsibleOrganisation(locationMonitoringPeople)
    if (responsibleOrgPeople.isEmpty()) {
      val regionsFound = locationMonitoringPeople.map { it.responsibleOrganisation?.takeIf(String::isNotBlank) ?: "Unknown" }.distinct()
      throw PersonWrongRegionException(regionsFound.joinToString(", "))
    }

    // filter on no position data
    val positionPeople = filterByHavingPositionData(responsibleOrgPeople)
    if (positionPeople.isEmpty()) {
      throw PersonNoPositionsException()
    }

    // filter on current order period
    val currentOrderPeople = filterByCurrentOrderPeriod(positionPeople)
    if (currentOrderPeople.isEmpty()) {
      throw PersonOutsideOrderPeriodException()
    }

    return PagedPeople(persons = currentOrderPeople, nextToken = null)
  }

  private fun fetchAllPeople(criteria: PeopleQueryCriteria, nextToken: String?): List<Person> {
    val people = mutableListOf<Person>()
    var cursor = nextToken
    do {
      val page = personRepository.searchPeople(criteria, cursor)
      people.addAll(page.persons)
      cursor = page.nextToken
    } while (cursor != null)
    return people
  }

  // return the latest person records by grouped date
  private fun latestRecords(records: List<Person>): List<Person> {
    val latestDate = records.mapNotNull { it.groupedDate }.maxOrNull()

    return records.filter { it.groupedDate == latestDate }
  }

  private fun filterByResponsibleOrganisation(people: List<Person>): List<Person> = if (responsibleOrganisations.isEmpty()) {
    people
  } else {
    people.filter { it.responsibleOrganisation in responsibleOrganisations }
  }

  private fun filterByEnforceableCondition(people: List<Person>): List<Person> = people.filter { it.enforceableCondition in Constants.ENFORCEABLE_CONDITIONS }

  private fun filterByHavingPositionData(people: List<Person>): List<Person> = people.filter { it.positionData?.hasPositionData ?: false }

  private fun filterByCurrentOrderPeriod(people: List<Person>): List<Person> {
    val today = LocalDate.now(ZoneOffset.UTC)
    return people.filter { person ->
      val startDate = person.orderStartDate?.atZone(ZoneOffset.UTC)?.toLocalDate()
      val endDate = person.orderEndDate?.atZone(ZoneOffset.UTC)?.toLocalDate()
      startDate != null && endDate != null && !today.isBefore(startDate) && !today.isAfter(endDate)
    }
  }

  fun getPersonById(personId: String): Person? = personRepository.findByPersonById(personId)

  fun getRawCaseloadByDeliusId(deliusId: String): List<RawCaseload> = personRepository.findRawCaseloadByDeliusId(deliusId)

  fun compareEmPerson(crn: String, personId: String): EmCompareResponse {
    val cprPerson = cprApiClient.getPersonByCrn(crn)
    val emPerson = personRepository.findEmPersonDetails(personId)
      ?: throw ResponseStatusException(NOT_FOUND, "Athena person $personId not found")
    val mainAddressIndex = cprPerson.addresses
      .indexOfFirst { it.status?.code.equals("M", ignoreCase = true) }
    val cprPostcode = cprPerson.addresses.getOrNull(mainAddressIndex)?.postcode
    val otherPostcodes = cprPerson.addresses
      .filterIndexed { index, _ -> index != mainAddressIndex }
      .mapNotNull { it.postcode }

    return EmCompareResponse(
      crn = crn,
      personId = personId,
      forename = compared(cprPerson.firstName, emPerson.forename),
      surname = compared(cprPerson.lastName, emPerson.surname),
      dateOfBirth = compared(cprPerson.dateOfBirth, emPerson.dateOfBirth?.toString()),
      postcode = comparePostcode(cprPostcode, otherPostcodes, emPerson.postcode),
    )
  }

  private fun compared(cpr: String?, athena: String?, removeSpaces: Boolean = false): ComparedValue {
    val normalisedCpr = normalise(cpr, removeSpaces)
    val normalisedAthena = normalise(athena, removeSpaces)
    return ComparedValue(
      cpr = cpr,
      athena = athena,
      matches = normalisedCpr != null && normalisedCpr == normalisedAthena,
    )
  }

  private fun comparePostcode(cpr: String?, otherCprPostcodes: List<String>, athena: String?): PostcodeComparedValue {
    val normalisedCpr = normalise(cpr, removeSpaces = true)
    val normalisedAthena = normalise(athena, removeSpaces = true)
    return PostcodeComparedValue(
      cpr = cpr,
      athena = athena,
      matches = normalisedCpr != null && normalisedCpr == normalisedAthena,
      matchesPreviousAddress = normalisedAthena != null &&
        otherCprPostcodes.any {
          normalise(it, removeSpaces = true) == normalisedAthena
        },
    )
  }

  private fun normalise(value: String?, removeSpaces: Boolean): String? = value
    ?.trim()
    ?.lowercase(Locale.UK)
    ?.let { if (removeSpaces) it.replace(" ", "") else it.replace(Regex("\\s+"), " ") }
    ?.takeIf(String::isNotEmpty)

  fun searchPeopleByPersonalDetails(request: PersonSearchRequest): List<Person> = personRepository.findByPersonalDetails(resolveSearchCriteria(request))
    .updateOutsidePeriodFlag()
    .sortedWith(
      compareByDescending<Person> { it.enforceableCondition == "location_monitoring" }
        .thenByDescending { it.positionData?.latestPositionGpsDate },
    )

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
    val mainAddressIndex = cprPerson.addresses
      .indexOfFirst { it.status?.code.equals("M", ignoreCase = true) }
    val cprPostcode = cprPerson.addresses.getOrNull(mainAddressIndex)?.postcode.normalisePostcode()
    val emPostcode = emPerson.postCode.normalisePostcode()
    val cprDob = cprPerson.dateOfBirth?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val emDob = emPerson.dob

    val exactNameMatch = cprName != null && cprName == emName
    val exactPostcodeMatch = cprPostcode != null && cprPostcode == emPostcode
    val exactDobMatch = cprDob != null && cprDob == emDob
    val nameScore = cprNames.maxOfOrNull { stringSimilarity(it, emName) } ?: 0.0
    val postcodeScore = stringSimilarity(cprPostcode, emPostcode)
    val postcodeMatchedPreviousAddress = postcodeScore < 1.0 &&
      emPostcode != null &&
      cprPerson.addresses
        .filterIndexed { index, _ -> index != mainAddressIndex }
        .any { it.postcode.normalisePostcode() == emPostcode }
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
      postcodeMatchedPreviousAddress = postcodeMatchedPreviousAddress,
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
    ?.replace(Regex("[^\\p{L}\\s-]"), "")
    ?.replace(Regex("[\\s-]+"), " ")
    ?.takeIf(String::isNotEmpty)

  private fun String?.normalisePostcode(): String? = this
    ?.trim()
    ?.lowercase(Locale.UK)
    ?.replace(Regex("\\s+"), "")
    ?.takeIf(String::isNotEmpty)

  private fun percentage(score: Double): Double = round(score * 10_000) / 100
}
