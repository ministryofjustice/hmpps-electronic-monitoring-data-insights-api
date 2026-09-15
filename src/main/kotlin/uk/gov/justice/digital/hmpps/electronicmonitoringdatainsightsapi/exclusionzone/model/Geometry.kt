package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "type",
  visible = true,
)
@JsonSubTypes(
  JsonSubTypes.Type(value = PolygonGeometry::class, name = "Polygon"),
  JsonSubTypes.Type(value = PointGeometry::class, name = "Point"),
)
sealed interface Geometry {
  val type: String
  val crs: CoordinateReferenceSystem
}
