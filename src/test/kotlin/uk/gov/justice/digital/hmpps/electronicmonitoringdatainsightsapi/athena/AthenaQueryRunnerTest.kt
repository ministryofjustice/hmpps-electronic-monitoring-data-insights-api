package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.*

class AthenaQueryRunnerTest {
  private val athenaClient = mockk<AthenaClient>()
  private lateinit var runner: AthenaQueryRunner

  @BeforeEach
  fun setUp() {
    runner = AthenaQueryRunner(
      athena = athenaClient,
      defaultDatabase = "test_db",
      outputLocation = "s3://test-output",
      pollIntervalMs = 1, // High speed for tests
      timeoutMs = 1000
    )
  }

  @Test
  fun `run should wait for query to succeed and map all results`() {
    val sql = "SELECT * FROM test"
    val executionId = "exec-123"

    // Mock Start
    every { athenaClient.startQueryExecution(any<StartQueryExecutionRequest>()) } returns
      StartQueryExecutionResponse.builder().queryExecutionId(executionId).build()

    // Mock Polling: RUNNING once, then SUCCEEDED
    every { athenaClient.getQueryExecution(any<GetQueryExecutionRequest>()) } returnsMany listOf(
      buildExecutionResponse(QueryExecutionState.RUNNING),
      buildExecutionResponse(QueryExecutionState.SUCCEEDED)
    )

    // Mock Results: Header row (index 0) and Data row (index 1)
    every { athenaClient.getQueryResults(any<GetQueryResultsRequest>()) } returns
      GetQueryResultsResponse.builder()
        .resultSet(ResultSet.builder().rows(
          buildRow("column_header"),
          buildRow("actual_value")
        ).build())
        .nextToken(null)
        .build()

    // Specify <String> to help Kotlin infer the mapper return type
    val results = runner.run<String>(sql) { it[0].varCharValue() }

    assertThat(results).containsExactly("actual_value")
    verify(exactly = 2) { athenaClient.getQueryExecution(any<GetQueryExecutionRequest>()) }
  }

  @Test
  fun `fetchPaged should use existing executionId from cursor and not start new query`() {
    val executionId = "existing-exec-id"
    val cursor = AthenaCursor(executionId, "token-abc").encode()

    every { athenaClient.getQueryResults(any<GetQueryResultsRequest>()) } returns
      GetQueryResultsResponse.builder()
        .resultSet(ResultSet.builder().rows(buildRow("paged-data")).build())
        .nextToken("token-xyz")
        .build()

    val result = runner.fetchPaged<String>(sql = "SELECT...", cursor = cursor) { it[0].varCharValue() }

    assertThat(result.items).containsExactly("paged-data")

    // Assertions to verify the "Pure" logic (no new query started)
    verify(exactly = 0) { athenaClient.startQueryExecution(any<StartQueryExecutionRequest>()) }
    verify(exactly = 0) { athenaClient.getQueryExecution(any<GetQueryExecutionRequest>()) }

    val decodedNext = AthenaCursor.decode(result.nextToken)
    assertThat(decodedNext?.queryExecutionId).isEqualTo(executionId)
    assertThat(decodedNext?.nextToken).isEqualTo("token-xyz")
  }

  @Test
  fun `run should throw IllegalStateException when query fails`() {
    every { athenaClient.startQueryExecution(any<StartQueryExecutionRequest>()) } returns
      StartQueryExecutionResponse.builder().queryExecutionId("fail-id").build()

    every { athenaClient.getQueryExecution(any<GetQueryExecutionRequest>()) } returns
      GetQueryExecutionResponse.builder().queryExecution(
        QueryExecution.builder().status(
          QueryExecutionStatus.builder()
            .state(QueryExecutionState.FAILED)
            .stateChangeReason("Access Denied")
            .build()
        ).build()
      ).build()

    val ex = assertThrows<IllegalStateException> {
      runner.run<String>("SELECT *") { "irrelevant" }
    }
    assertThat(ex.message).contains("FAILED : Access Denied")
  }

  // --- Helper Methods ---

  private fun buildExecutionResponse(state: QueryExecutionState) = GetQueryExecutionResponse.builder()
    .queryExecution(QueryExecution.builder()
      .status(QueryExecutionStatus.builder().state(state).build())
      .build())
    .build()

  private fun buildRow(value: String) = Row.builder()
    .data(Datum.builder().varCharValue(value).build())
    .build()
}