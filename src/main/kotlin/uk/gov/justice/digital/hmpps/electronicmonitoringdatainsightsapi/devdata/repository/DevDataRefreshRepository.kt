package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.repository

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataScript
import java.nio.charset.StandardCharsets

@Repository
class DevDataRefreshRepository(
  private val athenaQueryRunner: AthenaQueryRunner,
  private val resourcePatternResolver: ResourcePatternResolver,
  @param:Value("\${dev.test-data.script-location:classpath:dev-data-refresh/*.sql}")
  private val scriptLocation: String,
) {

  fun loadScripts(): List<DevDataScript> = resourcePatternResolver.getResources(scriptLocation)
    .filter { it.exists() && it.isReadable }
    .sortedBy { it.filename.orEmpty() }
    .map {
      DevDataScript(
        name = requireNotNull(it.filename) { "Dev data refresh script must have a filename" },
        sql = it.readText(),
      )
    }

  fun isDataMissing(script: DevDataScript): Boolean = athenaQueryRunner.run<Unit>(
    sql = script.sql,
    skipHeaderRow = true,
    mapper = {},
  ).isEmpty()

  fun execute(script: DevDataScript): String = athenaQueryRunner.execute(script.sql)

  private fun Resource.readText(): String = inputStream.use { String(it.readAllBytes(), StandardCharsets.UTF_8).trim() }
}
