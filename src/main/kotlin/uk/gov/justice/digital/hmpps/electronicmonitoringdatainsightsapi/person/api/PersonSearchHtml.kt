package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import org.springframework.web.util.HtmlUtils
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object PersonSearchHtml {
  private val timestampFormat = DateTimeFormatter.ofPattern("d MMM uuuu HH:mm 'UTC'").withZone(ZoneOffset.UTC)
  private val template = requireNotNull(javaClass.getResource("/templates/person-search.html")).readText()

  private fun value(value: Any?): String = HtmlUtils.htmlEscape(value?.toString()?.takeIf(String::isNotBlank) ?: "—")

  private fun timestamp(instant: Instant?): String = value(instant?.let(timestampFormat::format))

  fun render(response: PersonSearchResponse): String {
    val delius = response.deliusPerson
    val address = delius?.address
    val addressLines = listOfNotNull(
      address?.buildingName,
      listOfNotNull(address?.buildingNumber, address?.thoroughfareName)
        .filter(String::isNotBlank).joinToString(" "),
      address?.dependentLocality,
      address?.postTown,
      address?.county,
      address?.postcode,
    ).filter(String::isNotBlank)
    val deliusSection = if (delius == null) {
      "<p>No Delius details available. Supply a CRN to retrieve them.</p>"
    } else {
      """
      <dl>
        <dt>Forename</dt><dd>${value(delius.forename)}</dd>
        <dt>Surname</dt><dd>${value(delius.surname)}</dd>
        <dt>Date of birth</dt><dd>${value(delius.dateOfBirth)}</dd>
        <dt>CRN</dt><dd>${delius.identifiers.crns.takeIf { it.isNotEmpty() }?.joinToString("<br>") { value(it) } ?: "—"}</dd>
        <dt>Prison number</dt><dd>${delius.identifiers.prisonNumbers.takeIf { it.isNotEmpty() }?.joinToString("<br>") { value(it) } ?: "—"}</dd>
        <dt>PNC</dt><dd>${delius.identifiers.pncs.takeIf { it.isNotEmpty() }?.joinToString("<br>") { value(it) } ?: "—"}</dd>
        <dt>MON identifiers</dt><dd>${delius.identifiers.otherIdentifiers.takeIf { it.isNotEmpty() }?.joinToString("<br>") { value(it) } ?: "—"}</dd>
        <dt>Main address</dt><dd>${addressLines.takeIf { it.isNotEmpty() }?.joinToString("<br>") { value(it) } ?: "—"}</dd>
        <dt>No fixed abode</dt><dd>${value(address?.noFixedAbode?.let { if (it) "Yes" else "No" })}</dd>
      </dl>
      """.trimIndent()
    }
    val rows = response.athenaPeople.joinToString("\n") { person ->
      val identifiers = listOf(
        "nomisId" to person.nomisId,
        "pncId" to person.pncId,
        "deliusId" to person.deliusId,
      ).joinToString("") { (name, id) -> "<div class=\"identifier\"><span>$name</span> ${value(id)}</div>" }
      val athenaAddress = listOfNotNull(person.street, person.city, person.postCode)
        .filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.joinToString("<br>") { value(it) } ?: "—"
      """
      <tr>
        <td>${value(person.personId)}</td>
        <td>${value(person.personName)}</td>
        <td>${value(person.dob)}</td>
        <td>$athenaAddress</td>
        <td>${value(person.enforceableCondition)}</td>
        <td>${timestamp(person.orderStartDate)}<br>to ${timestamp(person.orderEndDate)}</td>
        <td>${person.positionData?.latestPositionGpsDate?.let(::timestamp) ?: "No GPS data"}</td>
        <td>$identifiers</td>
        <td>${value(person.orderId)}</td>
        <td>${if (person.isMonitored) "Yes" else "No"}</td>
      </tr>
      """.trimIndent()
    }
    val results = if (response.athenaPeople.isEmpty()) {
      "<p>No matching records found in the latest caseload snapshot.</p>"
    } else {
      """
      <div class="table-scroll" role="region" aria-label="Athena results" tabindex="0">
      <table>
        <thead><tr>
        <th scope="col">Person ID</th>
        <th scope="col">Name</th><th scope="col">DOB</th>
        <th scope="col">Address</th>
        <th scope="col">Enforceable condition</th>
        <th scope="col">Order period</th>
        <th scope="col">Latest GPS ping</th>
        <th scope="col">Identifiers</th>
        <th scope="col">Order ID</th>
        <th scope="col">Monitored</th>
        </tr></thead>
        <tbody>$rows</tbody>
      </table>
      </div>
      <p class="secondary">— = value not provided. All times are UTC.</p>
      """.trimIndent()
    }
    // Replace placeholders in one pass so values containing placeholder text remain untouched.
    val sections = mapOf("DELIUS" to deliusSection, "COUNT" to response.athenaPeople.size.toString(), "RESULTS" to results)
    return Regex("\\{\\{(DELIUS|COUNT|RESULTS)}}").replace(template) { sections.getValue(it.groupValues[1]) }
  }
}
