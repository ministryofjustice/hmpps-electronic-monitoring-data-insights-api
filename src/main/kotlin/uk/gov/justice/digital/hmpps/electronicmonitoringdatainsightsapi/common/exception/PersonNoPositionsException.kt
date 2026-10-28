package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonNoPositionsException :
  RuntimeException(
    "Person exists in EMDI but has no position data. Please contact EMDI team for more information.",
  )
