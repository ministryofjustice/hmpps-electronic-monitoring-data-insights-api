package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.service

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model.PduReportResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model.PduReportRow
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model.PersonOnProbationReportResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model.PersonOnProbationReportRow
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository.PduReportRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository.PersonOnProbationReportRepository

@Service
class ManagementInformationService(
  private val repository: PersonOnProbationReportRepository,
  private val pduRepository: PduReportRepository,
) {
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  fun getPdus(
    page: Int,
    pageSize: Int,
    sortField: String,
    sortDirection: String,
    pdu: String?,
    region: String?,
    popCount: Long?,
    lookBackDays: Int?,
  ): PduReportResponse {
    require(popCount == null || popCount >= 0) { "POPCount must be greater than or equal to 0" }
    require(lookBackDays == null || lookBackDays >= 0) { "lookBackDays must be greater than or equal to 0" }
    val offset = pageOffset(page, pageSize)
    val ascending = sortAscending(sortField, sortDirection)
    val totalElements = pduRepository.countReportRows(pdu, region, popCount, lookBackDays)
    return PduReportResponse(
      results = pduRepository.getPdus(pageSize, offset, ascending, pdu, region, popCount, lookBackDays).map {
        PduReportRow(
          pdu = it.pdu,
          region = it.region,
          searches = it.searches,
          pops = it.pops,
          lastViewed = it.lastViewed,
        )
      },
      page = page,
      size = pageSize,
      totalElements = totalElements,
      totalPages = totalPages(totalElements, pageSize),
    )
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  fun getPeopleOnProbation(
    page: Int,
    pageSize: Int,
    sortField: String,
    sortDirection: String,
    crn: String?,
    pdu: String?,
    region: String?,
    lookBackDayRange: String?,
  ): PersonOnProbationReportResponse {
    val offset = pageOffset(page, pageSize)
    val ascending = sortAscending(sortField, sortDirection)
    val (minDays, maxDays) = parseLookBackDayRange(lookBackDayRange)
    val totalElements = repository.countReportRows(crn, pdu, region, minDays, maxDays)
    return PersonOnProbationReportResponse(
      results = repository.getPeopleOnProbation(pageSize, offset, ascending, crn, pdu, region, minDays, maxDays).map {
        PersonOnProbationReportRow(
          crn = it.crn,
          region = it.region,
          searches = it.searches,
          users = it.users,
          lastViewed = it.lastViewed,
          tier = it.tier,
          pdu = it.pdu,
        )
      },
      page = page,
      size = pageSize,
      totalElements = totalElements,
      totalPages = totalPages(totalElements, pageSize),
    )
  }

  private fun parseLookBackDayRange(value: String?): Pair<Int?, Int?> {
    if (value == null) return null to null

    val message = "lookBackDayRange must be a range like 0-7 or 90-100000"
    val match = requireNotNull(LOOK_BACK_DAY_RANGE.matchEntire(value)) { message }
    val minDays = requireNotNull(match.groupValues[1].toIntOrNull()) { message }
    val maxDays = requireNotNull(match.groupValues[2].toIntOrNull()) { message }
    require(minDays <= maxDays) { message }
    return minDays to maxDays
  }

  private fun sortAscending(sortField: String, sortDirection: String): Boolean {
    require(sortField == "lastUpdated") { "sortField must be lastUpdated" }
    return Sort.Direction.fromString(sortDirection).isAscending
  }

  private fun pageOffset(page: Int, pageSize: Int): Long {
    require(page >= 0) { "page must be greater than or equal to 0" }
    require(pageSize > 0) { "pageSize must be greater than 0" }
    return page.toLong() * pageSize
  }

  private fun totalPages(totalElements: Int, pageSize: Int): Int = ((totalElements.toLong() + pageSize - 1) / pageSize).toInt()
}

private val LOOK_BACK_DAY_RANGE = Regex("""([0-9]+)-([0-9]+)""")
