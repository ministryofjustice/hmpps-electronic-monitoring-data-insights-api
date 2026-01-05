package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.repository.DeviceRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.LocationRepository
import java.util.UUID
import kotlin.time.ExperimentalTime
import java.time.Instant

@Service
class DeviceService(private val deviceRepository: DeviceRepository) {

  @OptIn(ExperimentalTime::class)
  fun findByCrn(crn: String): List<Device> {
    return deviceRepository.findByCrn(crn)
  }
}
