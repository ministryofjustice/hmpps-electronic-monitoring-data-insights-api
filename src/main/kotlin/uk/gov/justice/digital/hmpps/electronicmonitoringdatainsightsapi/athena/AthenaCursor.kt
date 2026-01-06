package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.slf4j.LoggerFactory
import java.util.Base64

data class AthenaCursor(val queryExecutionId: String, val nextToken: String?) {
  fun encode(): String {
    val raw = "$queryExecutionId|${nextToken ?: ""}"
    return Base64.getEncoder().encodeToString(raw.toByteArray())
  }

  companion object {
    private val log = LoggerFactory.getLogger(AthenaCursor::class.java)
    fun decode(cursor: String?): AthenaCursor? {
      if (cursor.isNullOrBlank()) return null
      return try {
        val decoded = String(Base64.getDecoder().decode(cursor))
        val parts = decoded.split("|")
        AthenaCursor(parts[0], parts[1].ifEmpty { null })
      } catch (e: Exception) {
        log.error("Failed to decode Athena cursor: {}", cursor, e)
        null
      }
    }
  }
}
