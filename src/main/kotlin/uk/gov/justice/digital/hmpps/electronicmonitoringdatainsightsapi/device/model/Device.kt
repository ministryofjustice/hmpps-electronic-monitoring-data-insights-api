package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model

import kotlin.time.ExperimentalTime
import java.time.Instant

data class Device @OptIn(ExperimentalTime::class) constructor(
  val deviceId: Int,
  val personId: Int,
  val deviceStatus: String? = null,
  val deviceDescription: String? = null,
  val deviceModelId: Int? = null,
  val deviceModelName: String? = null,
  val deviceSerialNumber: String? = null,
  val firmwareId: Int? = null,
  val firmwareLastUpdatedDate: Instant? = null,
  val firmwareVersion: String? = null,
  val operatorName: String? = null,
  val simcardTelephone: String? = null,
  val lastUpdatedAt: Instant? = Instant.now(),
  val locationId: Int? = null,
  val deactivationReasonName: String? = null,
  val deviceActivationDate: Instant? = null,
  val deviceDeactivationDate: Instant? = null,
)