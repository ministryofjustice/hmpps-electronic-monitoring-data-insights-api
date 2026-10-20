package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.Recurrence
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena.RestrictionZoneParsers
import java.time.DayOfWeek

class RestrictionZoneParsersTest {

  private val subject = RestrictionZoneParsers()

  @Test
  fun `should parse weekly recurrence for all days`() {
    val result = subject.parseRecurrence("week_1___0,1,2,3,4,5,6")

    assertThat(result).isEqualTo(
      Recurrence(
        weekFrequency = 1,
        daysOfWeek = setOf(
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY,
          DayOfWeek.SATURDAY,
          DayOfWeek.SUNDAY,
        ),
      ),
    )
  }

  @Test
  fun `should parse alternate week recurrence`() {
    val result = subject.parseRecurrence("week_2___0,2,4")

    assertThat(result).isEqualTo(
      Recurrence(
        weekFrequency = 2,
        daysOfWeek = setOf(
          DayOfWeek.MONDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.FRIDAY,
        ),
      ),
    )
  }

  @Test
  fun `should return null when week frequency is not numeric`() {
    assertThat(subject.parseRecurrence("week_x___0,1,2")).isNull()
  }

  @Test
  fun `should return null when all day values are invalid`() {
    assertThat(subject.parseRecurrence("week_1___10,11,12")).isNull()
  }
}
