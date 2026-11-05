package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation

import org.springframework.stereotype.Service

@Service
class ManagementInformationService(private val repository: PersonOnProbationReportRepository) {
  fun getPeopleOnProbation(): PersonOnProbationReportResponse = PersonOnProbationReportResponse(
    results = repository.getPeopleOnProbation().map {
      PersonOnProbationReportRow(it.crn, it.region, it.searches, it.users, it.lastViewed)
    },
  )
}
