package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.common.SingleRootFileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario

class AwsMockServer : WireMockServer(WIREMOCK_CONFIG) {
  companion object {
    private const val WIREMOCK_PORT = 8091
    private val WIREMOCK_CONFIG = WireMockConfiguration.wireMockConfig()
      .port(WIREMOCK_PORT)
      .fileSource(SingleRootFileSource("src/test/resources"))
  }

  fun stubStsAssumeRole() {
    stubFor(
      post(
        urlPathMatching("/"),
      ).withRequestBody(
        equalTo("Action=AssumeRole&Version=2011-06-15&RoleArn=testIAM&RoleSessionName=EMDIApiSession"),
      ).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/xml")
          .withBody(
            """
              <AssumeRoleResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
                <AssumeRoleResult>
                <SourceIdentity>mock</SourceIdentity>
                  <AssumedRoleUser>
                    <Arn>arn:aws:sts::123456789012:assumed-role/mock/mock</Arn>
                    <AssumedRoleId>ARO123EXAMPLE123:mock</AssumedRoleId>
                  </AssumedRoleUser>
                  <Credentials>
                    <AccessKeyId>ASIAIOSFODNN7EXAMPLE</AccessKeyId>
                    <SecretAccessKey>wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY</SecretAccessKey>
                    <SessionToken>
                     AQoDYXdzEPT//////////wEXAMPLEtc764bNrC9SAPBSM22wDOk4x4HIZ8j4FZTwdQW
                     LWsKWHGBuFqwAeMicRXmxfpSPfIeoIYRqTflfKD8YUuwthAx7mSEI/qkPpKPi/kMcGd
                     QrmGdeehM4IC1NtBmUpp2wUE8phUZampKsburEDy0KPkyQDYwT7WZ0wq5VSXDvp75YU
                     9HFvlRd8Tx6q6fE8YQcHNVXAkiY9q6d+xo0rKwT38xVqr7ZD0u0iPPkUL64lIZbqBAz
                     +scqKmlzm8FDrypNC9Yjc8fPOLn9FX9KSYvKTr4rvx3iSIlTJabIQwj2ICCR/oLxBA==
                    </SessionToken>
                    <Expiration>2019-11-09T13:34:41Z</Expiration>
                  </Credentials>
                  <PackedPolicySize>6</PackedPolicySize>
                </AssumeRoleResult>
                <ResponseMetadata>
                  <RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>
                </ResponseMetadata>
              </AssumeRoleResponse>
            """.trimIndent(),
          ),
      ),
    )
  }

  fun stubAthenaStartQueryExecution(queryExecutionId: String) {
    stubFor(
      post(
        urlPathEqualTo("/"),
      ).withHeader("X-Amz-Target", equalTo("AmazonAthena.StartQueryExecution"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "QueryExecutionId": "$queryExecutionId"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubAthenaGetQueryExecution(retryCount: Int, finalQueryExecutionState: String) {
    (1..retryCount).forEach {
      stubFor(
        post(
          urlPathEqualTo("/"),
        )
          .withHeader("X-Amz-Target", equalTo("AmazonAthena.GetQueryExecution"))
          .inScenario("GetQueryExecution")
          .whenScenarioStateIs(if (it == 1) Scenario.STARTED else "RETRY${it - 1}")
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withBody(
                """
            {
              "QueryExecution": {
                "QueryExecutionId": "",
                "Query": "",
                "StatementType": "",
                "ResultConfiguration": {
                  "OutputLocation": ""
                },
                "QueryExecutionContext": {
                  "Database": "",
                  "Catalog": ""
                },
                "Status": {
                  "State": "${if (it == retryCount) finalQueryExecutionState else "RUNNING"}",
                  "SubmissionDateTime": 0,
                  "CompletionDateTime": 0
                },
                "Statistics": {
                  "EngineExecutionTimeInMillis": 0,
                  "DataScannedInBytes": 0,
                  "TotalExecutionTimeInMillis": 0,
                  "QueryQueueTimeInMillis": 0,
                  "QueryPlanningTimeInMillis": 0
                },
                "WorkGroup": "AthenaAdmin"
              }
            }
                """.trimIndent(),
              ),
          )
          .willSetStateTo(if (it == retryCount) Scenario.STARTED else "RETRY$it"),
      )
    }
  }

  fun stubAthenaGetQueryResults(responseFile: String) {
    stubFor(
      post(
        urlPathEqualTo("/"),
      ).withHeader("X-Amz-Target", equalTo("AmazonAthena.GetQueryResults"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(responseFile),
        ),
    )
  }
}
