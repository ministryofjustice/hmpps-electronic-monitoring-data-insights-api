package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonNotFoundException :
  RuntimeException(
    "This person could not be found in EMDI using any of the Delius identifiers. Please contact EMDI support for more information.",
  )
