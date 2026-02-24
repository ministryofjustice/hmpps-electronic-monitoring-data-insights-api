package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation

/** Validates CRN String and returns Long */
fun String.toPersonId(): Long = this.toLongOrNull()
  ?: throw IllegalArgumentException("The CRN provided ($this) must be a numeric personId")

/** Validates LocationId String and returns Long */
fun String.toLocationId(): Long = this.toLongOrNull()
  ?: throw IllegalArgumentException("The LocationId provided ($this) must be a numeric locationId")

/** Validates ViolationId String conforms to SNOW ID */
fun String.toViolationId(): String {
  require(this.isNotBlank()) { "The ViolationId must not be blank" }
  require(this.matches(Regex("^[a-f0-9]{32}$"))) {
    "The violationId ($this) must be a 32-character lowercase hex string"
  }
  return this
}
/** Validates DeviceWearerId String conforms to SNOW ID */
fun String.toDeviceWearerId(): String {
  require(this.isNotBlank()) { "The DeviceWearerId must not be blank" }
  require(this.matches(Regex("^[a-f0-9]{32}$"))) {
    "The deviceWearerId ($this) must be a 32-character lowercase hex string"
  }
  return this
}

