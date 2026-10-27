package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonNotFoundException :
  RuntimeException(
    "This person could not be found in EMDI using any of the delius Ids",
  )
