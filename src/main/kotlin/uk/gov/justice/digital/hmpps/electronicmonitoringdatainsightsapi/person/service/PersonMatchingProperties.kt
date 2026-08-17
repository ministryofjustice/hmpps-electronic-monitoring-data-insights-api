package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "person-matching")
data class PersonMatchingProperties(
  val nameWeight: Double = 0.4,
  val postcodeWeight: Double = 0.3,
  val dateOfBirthWeight: Double = 0.3,
  val minimumStringSimilarity: Double = 0.5,
  val dateOfBirthToleranceDays: Long = 366,
) {
  init {
    require(nameWeight >= 0 && postcodeWeight >= 0 && dateOfBirthWeight >= 0) { "Person matching weights cannot be negative" }
    require(nameWeight + postcodeWeight + dateOfBirthWeight > 0) { "At least one person matching weight must be positive" }
    require(minimumStringSimilarity in 0.0..1.0) { "Minimum string similarity must be between 0 and 1" }
    require(dateOfBirthToleranceDays > 0) { "Date of birth tolerance must be positive" }
  }
}
