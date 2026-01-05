package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device

interface DeviceRepository {
  fun findByCrn(crn: String): List<Device>
}