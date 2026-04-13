package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.wiremock

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class AwsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val awsMockServer = AwsMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    awsMockServer.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    awsMockServer.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    awsMockServer.stop()
  }
}
