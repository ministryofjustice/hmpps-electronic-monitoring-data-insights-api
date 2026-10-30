package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonWrongEnforceableConditionException(val enforceableConditions: String) :
  RuntimeException(
    "This case is available to view however EMS does not have a location monitoring condition recorded as part of their GPS tag requirement. " +
      "" +
      "The following conditions are recorded on this case: $enforceableConditions " +
      "" +
      "If you think this is incorrect, please contact your regional EM team and ask them to add location monitoring as an enforceable condition to the record.",
  )
