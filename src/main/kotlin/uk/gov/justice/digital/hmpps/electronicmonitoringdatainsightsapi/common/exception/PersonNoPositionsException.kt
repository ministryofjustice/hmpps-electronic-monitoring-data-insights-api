package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonNoPositionsException(isMonitored: Boolean) :
  RuntimeException(
    "This case is available to view but no location data is currently available to display in the tool. <br>${
      if (isMonitored) {
        "If the tag has fitted within the last 48 hours, the tag may not be providing data yet, please check again the next day.  <br> " +
          "Otherwise, contact EMDI support and provide the CRN of the case you are trying to view and the code NO_POSITION_DATA_ERROR and they can investigate this for you. <br>"
      } else {
        "Please contact EMDI support and provide the CRN of the case you are trying to view and the code NO_POSITION_DATA_ERROR and they can investigate this for you. <br> "
      }
    } If you require this information urgently, please escalate your request through EMS.",
  )
