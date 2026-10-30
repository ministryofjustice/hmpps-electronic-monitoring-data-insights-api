package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonOutsideOrderPeriodException :
  RuntimeException(
    "This case is no longer available to view as the monitoring period is recorded as ended.  " +
      "" +
      "If you require information from before the end of the order or if the person is still wearing a tag and you require their location data, you can request this from EMS. ",
  )
