package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_MI_DATA
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model.PduReportResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model.PersonOnProbationReportResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.service.ManagementInformationService

@RestController
@RequestMapping("/management-information")
@Tag(name = "Management Information", description = "Management information reporting")
class ManagementInformationController(private val service: ManagementInformationService) {

  @GetMapping("/reference-data/pdu-list", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize(HAS_VIEW_MI_DATA)
  @Operation(summary = "Retrieve distinct recorded CRN PDUs in alphabetical order.")
  fun getPduList(): List<String> = service.getPduList()

  @GetMapping("/reference-data/region-list", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize(HAS_VIEW_MI_DATA)
  @Operation(summary = "Retrieve distinct recorded CRN probation areas in alphabetical order.")
  fun getRegionList(): List<String> = service.getRegionList()

  @GetMapping("/pdus", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize(HAS_VIEW_MI_DATA)
  @Operation(
    summary = "Retrieve activity by PDU.",
    description = "Returns one row per recorded CRN PDU and region. " +
      "Searches counts SEARCH_PERSON_BY_ID and VIEW_PERSON_LOCATIONS events; POPs counts distinct CRNs. " +
      "Each event contributes once to each distinct non-blank CRN PDU. " +
      "Events with null, empty or blank-only CRN PDUs are grouped under Unknown. " +
      "PDU and Region match full names ignoring case. POPCount matches the distinct CRN count. " +
      "lookBackDays filters by last viewed using rolling 24-hour days.\n\n" +
      "Example request `/management-information/pdus?PDU=Camden&Region=London&POPCount=5&lookBackDays=7`.",
  )
  fun getPdus(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "50") pageSize: Int,
    @RequestParam(defaultValue = "lastUpdated") sortField: String,
    @RequestParam(defaultValue = "desc") sortDirection: String,
    @RequestParam(name = "PDU") pdu: String?,
    @RequestParam(name = "Region") region: String?,
    @RequestParam(name = "POPCount") popCount: Long?,
    @RequestParam lookBackDays: Int?,
  ): PduReportResponse = service.getPdus(page, pageSize, sortField, sortDirection, pdu, region, popCount, lookBackDays)

  @GetMapping("/people-on-probation", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize(HAS_VIEW_MI_DATA)
  @Operation(
    summary = "Retrieve a list of people on probation and the number of searches against them.",
    description = "One row per recorded CRN, ordered by last viewed descending then CRN. " +
      "Searches counts SEARCH_PERSON_BY_ID and VIEW_PERSON_LOCATIONS, matching existing statistics. " +
      "Users counts distinct usernames across those events. Last viewed is their latest timestamp. " +
      "Region is the latest non-blank CRN probation areas value; it may contain multiple areas. " +
      "CRN, PDU and Region match full values ignoring case. " +
      "lookBackDayRange matches elapsed whole days since the last view, for example 0-7, 8-14 or 90-100000.\n\n" +
      "Example request `/management-information/people-on-probation?CRN=A123456&PDU=Camden&Region=London&lookBackDayRange=0-7`",
  )
  fun getPeopleOnProbation(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "50") pageSize: Int,
    @RequestParam(defaultValue = "lastUpdated") sortField: String,
    @RequestParam(defaultValue = "desc") sortDirection: String,
    @RequestParam(name = "CRN") crn: String?,
    @RequestParam(name = "PDU") pdu: String?,
    @RequestParam(name = "Region") region: String?,
    @RequestParam lookBackDayRange: String?,
  ): PersonOnProbationReportResponse = service.getPeopleOnProbation(page, pageSize, sortField, sortDirection, crn, pdu, region, lookBackDayRange)
}
