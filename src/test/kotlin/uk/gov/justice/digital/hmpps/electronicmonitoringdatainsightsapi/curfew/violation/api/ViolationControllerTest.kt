package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.service.ViolationService
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ViolationControllerTest {

  @Mock
  private lateinit var violationService: ViolationService

  @InjectMocks
  private lateinit var violationController: ViolationController

  @Test
  fun `findAllByConsumerIdAndTimespan should return 200 and paginated violations`() {
    // Arrange
    val consumerId = "7b2601d01bbf621072e76283b24bcbd5"
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
        outcomeReason = "Confirmed breach",
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
        outcomeReason = "Serious breach",
      ),
    )

    val pagedResult = PagedViolations(violations = mockViolations, nextToken = "next-token-456")

    // Act
    whenever(
      violationService.getViolationsForConsumer(consumerId, from, to, nextToken),
    ).thenReturn(pagedResult)

    // Assert
    val result = violationController.getViolations(
      consumerId = consumerId,
      from = from,
      to = to,
      nextToken = nextToken,
    )

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(pagedResult.violations.size).isEqualTo(2)
    assertThat(pagedResult.violations[0].violationId).isEqualTo("1234567890abcdef1234567890abcdef")
    assertThat(pagedResult.nextToken).isEqualTo("next-token-456")
  }

  @Test
  fun `findByConsumerIdAndViolationId should return 200 and single violation`() {
    // Arrange
    val consumerId = "abcdef1234567890abcdef1234567890"
    val violationId = "1234567890abcdef1234567890abcdef"

    val mockViolation = Violation(
      violationId = violationId,
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
      outcomeReason = "Confirmed breach",
    )

    whenever(violationService.getViolationForConsumer(consumerId, violationId)).thenReturn(mockViolation)

    // Act + Assert
    val result = violationController.getViolation(consumerId, violationId)

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body?.violationId).isEqualTo(violationId)
  }
}
