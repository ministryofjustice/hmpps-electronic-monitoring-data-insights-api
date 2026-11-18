package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.service

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
  fun getPdus(page: Int, pageSize: Int): PduReportResponse {
    val offset = pageOffset(page, pageSize)
    val totalElements = pduRepository.countReportRows()
    return PduReportResponse(
      results = pduRepository.getPdus(pageSize, offset).map {
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
  fun getPeopleOnProbation(page: Int, pageSize: Int): PersonOnProbationReportResponse {
    val offset = pageOffset(page, pageSize)
    val totalElements = repository.countReportRows()
    return PersonOnProbationReportResponse(
      results = repository.getPeopleOnProbation(pageSize, offset).map {
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

  private fun pageOffset(page: Int, pageSize: Int): Long {
    require(page >= 0) { "page must be greater than or equal to 0" }
    require(pageSize > 0) { "pageSize must be greater than 0" }
    return page.toLong() * pageSize
  }

  private fun totalPages(totalElements: Int, pageSize: Int): Int = ((totalElements.toLong() + pageSize - 1) / pageSize).toInt()
}
