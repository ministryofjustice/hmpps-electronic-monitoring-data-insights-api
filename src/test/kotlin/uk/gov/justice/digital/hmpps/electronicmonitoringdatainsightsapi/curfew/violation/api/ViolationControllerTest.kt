package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.service.ViolationService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import java.time.Instant

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ViolationController::class)
class ViolationControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockkBean
  private lateinit var violationService: ViolationService

  @Test
  fun `findAllByCrnAndTimespan should return 200 and paginated violations`() {
    // Arrange
    val crn = "7b2601d01bbf621072e76283b24bcbd5"
    val from = Instant.parse("2026-10-01T10:00:00Z")
    val to = Instant.parse("2026-10-01T11:00:00Z")
    val nextToken = "token123"

    val mockViolations = listOf(
      Violation(
        violationId = "1234567890abcdef1234567890abcdef",
        deviceWearer = "abcdef1234567890abcdef1234567890",
        createdDate = Instant.parse("2026-02-15T08:30:00Z"),
        category = "Non-Fitted Device Violation",
        duration = Instant.parse("2026-02-15T00:10:00Z"),
        start = "2026-02-15T08:20:00Z",
        end = Instant.parse("2026-02-15T08:30:00Z"),
        state = "Open",
        active = "true",
        description = "Device not fitted during curfew period",
        responseAction = "Warning Issued",
        reasonableExcuse = "No",
        authorisedAbsence = "No",
        includedInTotalAtvCalculation = "Yes",
        outForEntireCurfewPeriod = "No",
        outcomeReason = "Confirmed breach"
      ),
      Violation(
        violationId = "fedcba0987654321fedcba0987654321",
        deviceWearer = "abcdef1234567890abcdef1234567890",
        createdDate = Instant.parse("2026-02-16T21:45:00Z"),
        category = "Home Detention Curfew Violation",
        duration = Instant.parse("2026-02-16T00:25:00Z"),
        start = "2026-02-16T21:20:00Z",
        end = Instant.parse("2026-02-16T21:45:00Z"),
        state = "Closed",
        active = "false",
        description = "Subject outside permitted zone",
        responseAction = "Escalated",
        reasonableExcuse = "No",
        authorisedAbsence = "No",
        includedInTotalAtvCalculation = "Yes",
        outForEntireCurfewPeriod = "Yes",
        outcomeReason = "Serious breach"
      )
    )

    val pagedResult = PagedViolations(violations = mockViolations, nextToken = "next-token-456")

    // Act
    every {
      violationService.findAllByCrnAndTimespan(crn, from, to, nextToken)
    } returns pagedResult

    // Assert
    mockMvc.perform(
      get("/people/$crn/curfew/violations")
        .param("from", from.toString())
        .param("to", to.toString())
        .param("nextToken", nextToken),
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.violations.length()").value(2))
      .andExpect(jsonPath("$.violations[0].violationId").value("1234567890abcdef1234567890abcdef"))
      .andExpect(jsonPath("$.nextToken").value("next-token-456"))
  }

  @Test
  fun `findByCrnAndId should return 200 and single location list`() {
    // Arrange
    val crn = "abcdef1234567890abcdef1234567890"
    val violationId = "1234567890abcdef1234567890abcdef"

    val mockViolation = listOf(
      Violation(
        violationId = "1234567890abcdef1234567890abcdef",
        deviceWearer = "abcdef1234567890abcdef1234567890",
        createdDate = Instant.parse("2026-02-15T08:30:00Z"),
        category = "Non-Fitted Device Violation",
        duration = Instant.parse("2026-02-15T00:10:00Z"),
        start = "2026-02-15T08:20:00Z",
        end = Instant.parse("2026-02-15T08:30:00Z"),
        state = "Open",
        active = "true",
        description = "Device not fitted during curfew period",
        responseAction = "Warning Issued",
        reasonableExcuse = "No",
        authorisedAbsence = "No",
        includedInTotalAtvCalculation = "Yes",
        outForEntireCurfewPeriod = "No",
        outcomeReason = "Confirmed breach"
      ))

    // Act
    every { violationService.findByCrnAndId(crn, violationId) } returns mockViolation

    // Assert
    mockMvc.perform(get("/people/$crn/curfew/violations/$violationId"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$[0].violationId").value(violationId))
  }
}

