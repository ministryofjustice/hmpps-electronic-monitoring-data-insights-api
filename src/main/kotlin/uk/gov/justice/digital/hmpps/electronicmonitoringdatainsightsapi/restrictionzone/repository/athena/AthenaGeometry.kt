package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.CoordinateReferenceSystem

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AthenaGeometry.Point::class, name = "Point"),
  JsonSubTypes.Type(value = AthenaGeometry.Polygon::class, name = "Polygon"),
)
internal sealed interface AthenaGeometry {
  val crs: CoordinateReferenceSystem

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Point(
    override val crs: CoordinateReferenceSystem,
    val coordinates: List<Double>,
  ) : AthenaGeometry

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Polygon(
    override val crs: CoordinateReferenceSystem,
    val coordinates: List<List<List<Double>>>,
  ) : AthenaGeometry
}
