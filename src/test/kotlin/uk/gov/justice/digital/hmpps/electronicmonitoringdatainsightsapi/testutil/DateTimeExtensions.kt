package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.testutil

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun LocalDateTime.norm() = this.truncatedTo(ChronoUnit.MILLIS)
