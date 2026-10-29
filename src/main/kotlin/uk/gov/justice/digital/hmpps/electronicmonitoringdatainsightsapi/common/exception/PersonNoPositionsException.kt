package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonNoPositionsException(isMonitored: Boolean) :
  RuntimeException(
    "Person exists in EMDI but has no position data. The person is showing as ${if (isMonitored) "monitored in EM." else "not monitored in EM."} Please contact EMDI team for more information.",
  )
