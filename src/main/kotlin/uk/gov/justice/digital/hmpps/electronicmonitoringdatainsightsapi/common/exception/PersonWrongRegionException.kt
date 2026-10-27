package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonWrongRegionException(region: String) :
  RuntimeException(
    "Person exists in EMDI but is associated with region $region which is not trial region.",
  )
