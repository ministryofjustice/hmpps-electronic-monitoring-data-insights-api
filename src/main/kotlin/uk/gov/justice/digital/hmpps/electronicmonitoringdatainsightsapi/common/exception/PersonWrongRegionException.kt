package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

class PersonWrongRegionException(region: String) :
  RuntimeException(
    "This case is available to view however EMS have recorded this person as being in $region which is not currently part of the EMDI pilot.  " +
      "" +
      "If you think this region is incorrect, please contact your regional EM team for help in updating this.  " +
      "" +
      "East of England -  EoEPS.EMSRegional@justice.gov.uk   " +
      "London -  LondonPS.EMQueries@justice.gov.uk  " +
      "West Midlands - WMPS.ElectronicMonitoring@justice.gov.uk  ",
  )
