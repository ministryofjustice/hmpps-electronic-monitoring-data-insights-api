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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository.ReferenceDataRepository

@Service
class ManagementInformationService(
  private val repository: PersonOnProbationReportRepository,
  private val pduRepository: PduReportRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun getPduList(): List<String> = referenceDataRepository.getPduList()

  fun getRegionList(): List<String> = referenceDataRepository.getRegionList()

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  fun getPdus(page: Int, pageSize: Int, sortField: String, sortDirection: String): PduReportResponse {
    val offset = pageOffset(page, pageSize)
    val ascending = sortAscending(sortField, sortDirection)
    val totalElements = pduRepository.countReportRows()
    return PduReportResponse(
      results = pduRepository.getPdus(pageSize, offset, ascending).map {
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
  fun getPeopleOnProbation(page: Int, pageSize: Int, sortField: String, sortDirection: String): PersonOnProbationReportResponse {
    val offset = pageOffset(page, pageSize)
    val ascending = sortAscending(sortField, sortDirection)
    val totalElements = repository.countReportRows()
    return PersonOnProbationReportResponse(
      results = repository.getPeopleOnProbation(pageSize, offset, ascending).map {
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
