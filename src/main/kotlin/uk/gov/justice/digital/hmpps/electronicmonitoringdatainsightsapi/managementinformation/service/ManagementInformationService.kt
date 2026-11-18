package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.service

import org.springframework.stereotype.Service
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
  fun getPdus(): PduReportResponse = PduReportResponse(
    results = pduRepository.getPdus().map {
      PduReportRow(
        pdu = it.pdu,
        region = it.region,
        searches = it.searches,
        pops = it.pops,
        lastViewed = it.lastViewed,
      )
    },
  )

  fun getPeopleOnProbation(): PersonOnProbationReportResponse = PersonOnProbationReportResponse(
    results = repository.getPeopleOnProbation().map {
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
  )
}
