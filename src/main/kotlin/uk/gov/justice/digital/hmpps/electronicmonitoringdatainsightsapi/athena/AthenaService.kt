import kotlinx.datetime.LocalDateTime
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest

@Service
class AthenaService(
  private val athenaClient: AthenaClient = AthenaClient.create(),
) {
  // TODO: Externalise this to configuration
  private val outputLocation = "https://eu-west-2.console.aws.amazon.com/s3/buckets/emds-test-athena-query-results-20240923095933297100000013/"

  fun runIncrementalQuery(tableName: String, lastSync: LocalDateTime): String {
    // Construct query using the watermark timestamp
    val sql = "SELECT * FROM $tableName WHERE last_updated_at > '$lastSync'"

    val startRequest = StartQueryExecutionRequest.builder()
      .queryString(sql)
      .resultConfiguration { it.outputLocation(outputLocation) }
      .build()

    return athenaClient.startQueryExecution(startRequest).queryExecutionId()
  }

  // Polling logic to check if the query is finished
  fun isQueryComplete(queryExecutionId: String): Boolean {
    val request = GetQueryExecutionRequest.builder()
      .queryExecutionId(queryExecutionId)
      .build()

    val status = athenaClient.getQueryExecution(request).queryExecution().status().state()
    return status == QueryExecutionState.SUCCEEDED
  }
}
