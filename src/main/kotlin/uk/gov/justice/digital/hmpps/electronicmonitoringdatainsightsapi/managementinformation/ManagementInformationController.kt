package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_MI_DATA
import java.time.Instant

@RestController
@RequestMapping("/management-information")
@Tag(name = "Management Information", description = "Management information reporting")
class ManagementInformationController(private val repository: PersonOnProbationReportRepository) {

  @GetMapping("/people-on-probation", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize(HAS_VIEW_MI_DATA)
  @Operation(
    summary = "Retrieve a list of people on probation and the number of searches against them.",
    description = "One row per recorded CRN, ordered by last viewed descending then CRN. " +
      "Searches counts SEARCH_PERSON_BY_ID and VIEW_PERSON_LOCATIONS, matching existing statistics. " +
      "Users counts distinct usernames across those events. Last viewed is their latest timestamp. " +
      "Region is the latest non-blank CRN probation areas value; it may contain multiple areas. ",
  )
  fun getPeopleOnProbation(): PersonOnProbationReportResponse = PersonOnProbationReportResponse(
    results = repository.getPeopleOnProbation().map {
      PersonOnProbationReportRow(it.crn, it.region, it.searches, it.users, it.lastViewed)
    },
  )
}

data class PersonOnProbationReportResponse(
  val results: List<PersonOnProbationReportRow>,
)

data class PersonOnProbationReportRow(
  val crn: String,
  val region: String?,
  val searches: Long,
  val users: Long,
  val lastViewed: Instant,
)
