package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataRefreshStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataScript
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.repository.DevDataRefreshRepository

class DevDataRefreshServiceTest {

  private val repository = mockk<DevDataRefreshRepository>()
  private val service = DevDataRefreshService(repository)

  private val checkScript = DevDataScript("00-check.sql", "SELECT true")
  private val firstRefreshScript = DevDataScript("01-first.sql", "SELECT 1")
  private val secondRefreshScript = DevDataScript("02-second.sql", "SELECT 2")

  @Test
  fun `refresh should skip remaining scripts when check script says data exists`() {
    every { repository.loadScripts() } returns listOf(checkScript, firstRefreshScript, secondRefreshScript)
    every { repository.isDataMissing(checkScript) } returns false

    val result = service.refresh()

    assertThat(result.status).isEqualTo(DevDataRefreshStatus.SKIPPED)
    assertThat(result.checkScript).isEqualTo("00-check.sql")
    assertThat(result.executedScripts).isEmpty()
    assertThat(result.skippedScripts).containsExactly("01-first.sql", "02-second.sql")
    verify(exactly = 0) { repository.execute(any()) }
  }

  @Test
  fun `refresh should execute remaining scripts in order when check script says data is missing`() {
    every { repository.loadScripts() } returns listOf(checkScript, firstRefreshScript, secondRefreshScript)
    every { repository.isDataMissing(checkScript) } returns true
    every { repository.execute(firstRefreshScript) } returns "execution-1"
    every { repository.execute(secondRefreshScript) } returns "execution-2"

    val result = service.refresh()

    assertThat(result.status).isEqualTo(DevDataRefreshStatus.COMPLETED)
    assertThat(result.executedScripts).containsExactly("01-first.sql", "02-second.sql")
    assertThat(result.skippedScripts).isEmpty()
    verify(exactly = 1) { repository.execute(firstRefreshScript) }
    verify(exactly = 1) { repository.execute(secondRefreshScript) }
  }
}
