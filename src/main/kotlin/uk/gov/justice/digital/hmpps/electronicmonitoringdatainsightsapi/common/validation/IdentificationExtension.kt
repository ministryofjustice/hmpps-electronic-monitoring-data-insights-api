package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation

/** Validates CRN String and returns Long */
fun String.toPersonId(): Long = this.toLongOrNull()
  ?: throw IllegalArgumentException("The CRN provided ($this) must be a numeric personId")

/** Validates LocationId String and returns Long */
fun String.toLocationId(): Long = this.toLongOrNull()
  ?: throw IllegalArgumentException("The LocationId provided ($this) must be a numeric locationId")
