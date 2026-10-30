package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonNotFoundException :
  RuntimeException(
    "This person could not be found in EMDI  <br>" +
      "This could be because the tag has been fitted within the last 48 hours, please check again the next day. The identifying information from Delius cannot be matched to identifying information in the EM data. <br>" +
      "<br>" +
      "This can sometimes be corrected by EMDI support who can explain how to add a new identifier into the record.  <br>" +
      "<br>" +
      "Please contact  EMDI support and provide the CRN of the case you are trying to view and the Person Not Found error code.  <br>",
  )
