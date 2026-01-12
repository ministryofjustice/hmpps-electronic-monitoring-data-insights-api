package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.Datum
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.model.PaginatedResult
import kotlin.math.min

@Component
class AthenaQueryRunner(
  private val athena: AthenaClient,
  @Value("\${athena.defaultDatabase}") private val defaultDatabase: String,
  @Value("\${athena.outputLocation}") private val outputLocation: String,
  @Value("\${athena.pollIntervalMs:500}") private val pollIntervalMs: Long,
  @Value("\${athena.timeoutMs:60000}") private val timeoutMs: Long,
) {

  fun <T> run(
    sql: String,
    database: String = defaultDatabase,
    skipHeaderRow: Boolean = true,
    mapper: (List<Datum>) -> T,
  ): List<T> {
    val executionId = startQuery(sql, database)
    waitForCompletion(executionId)
    return fetchAllResults(executionId, skipHeaderRow, mapper)
  }

  /**
   * If [cursor] is null, starts a new query.
   * If [cursor] is present, fetches the next batch for the existing query.
   */
  fun <T> fetchPaged(
    sql: String,
    database: String = defaultDatabase,
    cursor: String? = null,
    pageSize: Int = 100,
    mapper: (List<Datum>) -> T,
  ): PaginatedResult<T> {
    val athenaCursor = AthenaCursor.decode(cursor)

    val executionId = if (athenaCursor == null) {
      val id = startQuery(sql, database)
      waitForCompletion(id)
      id
    } else {
      athenaCursor.queryExecutionId
    }

    val response = athena.getQueryResults(
      GetQueryResultsRequest.builder()
        .queryExecutionId(executionId)
        .nextToken(athenaCursor?.nextToken)
        .maxResults(pageSize)
        .build(),
    )

    val rows = response.resultSet().rows()
    // Header Logic: Only skip the header row if this is the very first page of a new query
    val isFirstPageOfQuery = athenaCursor?.nextToken == null
    val startIdx = if (isFirstPageOfQuery) 1 else 0

    val items = rows.drop(startIdx).map { mapper(it.data()) }

    // Create the next cursor if more data exists
    val nextToken = response.nextToken()
    val nextCursor = nextToken?.let { AthenaCursor(executionId, it).encode() }

    return PaginatedResult(items, nextCursor)
  }

  private fun startQuery(sql: String, database: String): String {
    val req = StartQueryExecutionRequest.builder()
      .queryString(sql)
      .queryExecutionContext(
        QueryExecutionContext.builder()
          .database(database)
          .build(),
      )
      .resultConfiguration(
        ResultConfiguration.builder()
          .outputLocation(outputLocation)
          .build(),
      )
      .build()

    return athena.startQueryExecution(req).queryExecutionId()
  }

  private fun waitForCompletion(executionId: String) {
    val deadline = System.currentTimeMillis() + timeoutMs
    var sleepMs = pollIntervalMs

    while (true) {
      val exec = athena.getQueryExecution(
        GetQueryExecutionRequest.builder().queryExecutionId(executionId).build(),
      ).queryExecution()

      when (exec.status().state()) {
        QueryExecutionState.SUCCEEDED -> return
        QueryExecutionState.FAILED, QueryExecutionState.CANCELLED -> {
          val reason = exec.status().stateChangeReason() ?: "Unknown reason"
          throw IllegalStateException("Athena query $executionId ${exec.status().state()} : $reason")
        }
        else -> {
          if (System.currentTimeMillis() > deadline) {
            throw IllegalStateException("Athena query $executionId timed out after ${timeoutMs}ms")
          }
          Thread.sleep(sleepMs)
          sleepMs = min((sleepMs * 1.2).toLong().coerceAtLeast(pollIntervalMs), 2000L)
        }
      }
    }
  }

  private fun <T> fetchAllResults(executionId: String, skipHeaderRow: Boolean, mapper: (List<Datum>) -> T): List<T> {
    val out = mutableListOf<T>()
    var nextToken: String? = null
    var firstPage = true

    do {
      val resp = athena.getQueryResults(
        GetQueryResultsRequest.builder()
          .queryExecutionId(executionId)
          .nextToken(nextToken)
          .build(),
      )

      val rows = resp.resultSet().rows()
      val startIdx = if (firstPage && skipHeaderRow) 1 else 0
      for (i in startIdx until rows.size) out += mapper(rows[i].data())

      nextToken = resp.nextToken()
      firstPage = false
    } while (nextToken != null)

    return out
  }
}
