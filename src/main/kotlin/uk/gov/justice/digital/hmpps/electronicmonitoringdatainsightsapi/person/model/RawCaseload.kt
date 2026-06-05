package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import com.fasterxml.jackson.annotation.JsonProperty

data class RawCaseload(
  @param:JsonProperty("grouped_date")
  val groupedDate: String? = null,
  @param:JsonProperty("unique_device_wearer_id")
  val uniqueDeviceWearerId: String? = null,
  @param:JsonProperty("first_name")
  val firstName: String? = null,
  @param:JsonProperty("last_name")
  val lastName: String? = null,
  @param:JsonProperty("date_of_birth")
  val dateOfBirth: String? = null,
  @param:JsonProperty("house_number_and_street_name")
  val houseNumberAndStreetName: String? = null,
  @param:JsonProperty("city_or_town")
  val cityOrTown: String? = null,
  val county: String? = null,
  val country: String? = null,
  val postcode: String? = null,
  @param:JsonProperty("nomis_id")
  val nomisId: String? = null,
  @param:JsonProperty("pnc_id")
  val pncId: String? = null,
  @param:JsonProperty("delius_id")
  val deliusId: String? = null,
  @param:JsonProperty("mdss_person_id")
  val mdssPersonId: String? = null,
  @param:JsonProperty("order_id")
  val orderId: String? = null,
  @param:JsonProperty("order_start_date")
  val orderStartDate: String? = null,
  @param:JsonProperty("order_commencement_date")
  val orderCommencementDate: String? = null,
  @param:JsonProperty("order_end_date")
  val orderEndDate: String? = null,
  @param:JsonProperty("order_type")
  val orderType: String? = null,
  @param:JsonProperty("order_type_description")
  val orderTypeDescription: String? = null,
  @param:JsonProperty("order_type_detail")
  val orderTypeDetail: String? = null,
  @param:JsonProperty("responsible_organisation")
  val responsibleOrganisation: String? = null,
  @param:JsonProperty("responsible_officer_name")
  val responsibleOfficerName: String? = null,
  @param:JsonProperty("is_monitored")
  val isMonitored: String? = null,
  @param:JsonProperty("enforceable_condition")
  val enforceableCondition: String? = null,
  @param:JsonProperty("__datetime_added")
  val datetimeAdded: String? = null,
)
