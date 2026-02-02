package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException

class AthenaDeviceRepositoryTest {

  private val properties = AwsProperties(
    region = Region.EU_WEST_2,
    athena = AthenaProperties(
      role = null,
      mdssDatabase = "allied_mdss_test",
      fmsDatabase = "serco_fms_test",
      defaultDatabase = "allied_mdss_test",
      outputLocation = "s3://bucket/output",
      workgroup = "wg",
      pollIntervalMs = 500,
      timeoutMs = 60000,
    ),
  )

  private val runner = mockk<AthenaQueryRunner>()
  private val database = "test_db"
  private val repository = AthenaDeviceRepository(runner, properties)

  @Test
  fun `findByCrn should build SQL with correct personId and call runner`() {
    // Arrange
    val crn = "12345"
    val sqlSlot = slot<String>()

    // Act
    every {
      runner.run(capture(sqlSlot), eq(database), any(), any<(List<Datum>) -> Any>(), any())
    } returns emptyList<Nothing>()
    repository.findByCrn(crn)

    // / Assert
    assertThat(sqlSlot.captured).contains("person_id = CAST(? AS BIGINT)")
    assertThat(sqlSlot.captured).contains("WITH latest_device AS")
    assertThat(sqlSlot.captured).contains("LEFT JOIN latest_activation")
  }

  @Test
  fun `mapRow should correctly map all Athena columns to Device object`() {
    // Arrange
    val mockRow = listOf(
      datum("101"), // COL_DEVICE_ID
      datum("GPS Tag"), // COL_DEVICE_DESC
      datum("1"), // COL_DEVICE_MODEL_ID
      datum("Model X"), // COL_DEVICE_MODEL_NAME
      datum("SN-999"), // COL_DEVICE_SERIAL_NUMBER
      datum("ACTIVE"), // COL_DEVICE_STATUS
      datum("5"), // COL_FIRMWARE_ID
      datum("2026-10-27 10:00:00.000000"), // COL_FIRMWARE_LAST_UPDATED_DATE
      datum("v1.0"), // COL_FIRMWARE_VERSION
      datum("Vodafone"), // COL_OPERATOR_NAME
      datum("07123456789"), // COL_SIM_NUMBER
      datum("2026-10-27 12:00:00.000000"), // COL_LAST_UPDATED_DATE
      datum("54321"), // COL_PERSON_ID
      datum("99"), // COL_LOCATION_ID
      datum("End of sentence"), // COL_DEACTIVATION_REASON
      datum("2026-01-01 09:00:00.000000"), // COL_DEVICE_ACTIVATION_DATE
      datum(null), // COL_DEVICE_DEACTIVATION_DATE
    )

    // Act
    every { runner.run<Any>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Any
      listOf(mapper(mockRow))
    }
    val result = repository.findByCrn("54321")
    val device = result[0]

    // Assert
    assertThat(device.deviceId).isEqualTo(101)
    assertThat(device.personId).isEqualTo(54321)
    assertThat(device.deviceStatus).isEqualTo("ACTIVE")
    assertThat(device.simcardTelephone).isEqualTo("07123456789")
  }

  @Test
  fun `mapRow should throw DataIntegrityException when required field is missing`() {
    // Arrange
    val invalidRow = List(17) { datum(null) } // All nulls
    val mapperSlot = slot<(List<Datum>) -> Any>()

    // Act
    every { runner.run<Any>(any(), any(), any(), capture(mapperSlot), any()) } answers {
      listOf(mapperSlot.captured(invalidRow))
    }

    // Assert
    assertThrows<DataIntegrityException> {
      repository.findByCrn("12345")
    }
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
