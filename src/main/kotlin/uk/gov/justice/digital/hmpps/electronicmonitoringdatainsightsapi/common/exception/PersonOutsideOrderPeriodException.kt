package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonOutsideOrderPeriodException :
  RuntimeException(
    "Person exists in EMDI but has no order covering the current date. Please contact EMDI team for more information.",
  )
