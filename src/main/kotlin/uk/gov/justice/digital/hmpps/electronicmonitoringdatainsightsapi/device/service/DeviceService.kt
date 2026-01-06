package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.repository.DeviceRepository
import kotlin.time.ExperimentalTime

@Service
class DeviceService(private val deviceRepository: DeviceRepository) {

  @OptIn(ExperimentalTime::class)
  fun findByCrn(crn: String): List<Device> = deviceRepository.findByCrn(crn)
}
