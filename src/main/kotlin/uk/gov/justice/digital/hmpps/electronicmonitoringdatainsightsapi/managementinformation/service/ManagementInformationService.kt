package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model.PersonOnProbationReportResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model.PersonOnProbationReportRow
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository.PersonOnProbationReportRepository

@Service
class ManagementInformationService(private val repository: PersonOnProbationReportRepository) {
  fun getPeopleOnProbation(): PersonOnProbationReportResponse = PersonOnProbationReportResponse(
    results = repository.getPeopleOnProbation().map {
      PersonOnProbationReportRow(
        crn = it.crn,
        region = it.region,
        searches = it.searches,
        users = it.users,
        lastViewed = it.lastViewed,
        tier = "", // TODO - populate tier
        pdu = "", // TODO - populate pdu
      )
    },
  )
}
