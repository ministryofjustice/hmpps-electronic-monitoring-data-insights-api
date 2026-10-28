package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonWrongEnforceableConditionException(enforceableConditions: String) :
  RuntimeException(
    "Person exists in EMDI but does not have an active location monitoring enforceable condition. Conditions found: $enforceableConditions Please contact your regional EM team for more information.",
  )
