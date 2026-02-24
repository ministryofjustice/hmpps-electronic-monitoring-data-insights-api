package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.repository.ViolationRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.service.ViolationService
import java.time.Instant

class ViolationServiceTest {

  private val violationRepository = mockk<ViolationRepository>()
  private val violationService = ViolationService(violationRepository)

  @Test
  fun `findAllByCrnAndTimespan should call repository when dates are valid`() {
    // Arrange
    val crn = "abcdef1234567890abcdef1234567890"
    val from = Instant.parse("2023-10-01T10:00:00Z")
    val to = Instant.parse("2023-10-01T11:00:00Z")
    val nextToken = "some-token"


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
      violationRepository.findAllByCrnAndTimespan(crn, from, to, nextToken)
    } returns pagedResult
    val result = violationService.findAllByCrnAndTimespan(crn, from, to, nextToken)

    // Assert
    assertThat(result).isEqualTo(pagedResult)
    verify(exactly = 1) { violationRepository.findAllByCrnAndTimespan(crn, from, to, nextToken) }
  }

  @Test
  fun `findAllByCrnAndTimespan should throw exception when 'from' is after 'to'`() {
    // Arrange
    val crn = "abcdef1234567890abcdef1234567890"
    val from = Instant.parse("2023-10-01T12:00:00Z")
    val to = Instant.parse("2023-10-01T11:00:00Z") // 1 hour earlier than 'from'

    // Act
    val exception = assertThrows<IllegalArgumentException> {
      violationService.findAllByCrnAndTimespan(crn, from, to, null)
    }

    // Assert
    assertThat(exception.message).contains("must be before or equal to")
    verify(exactly = 0) { violationRepository.findAllByCrnAndTimespan(any(), any(), any(), any()) }
  }

  @Test
  fun `findByCrnAndId should call repository and return result`() {
    // Arrange
    val crn = "abcdef1234567890abcdef1234567890"
    val violationId = "1234567890abcdef1234567890abcdef"

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
      ))

    every { violationRepository.findByCrnAndId(crn, violationId) } returns mockViolations

    // Act
    val result = violationService.findByCrnAndId(crn, violationId)

    // Assert
    assertThat(result).isEqualTo(mockViolations)
    verify(exactly = 1) { violationRepository.findByCrnAndId(crn, violationId) }
  }
}
