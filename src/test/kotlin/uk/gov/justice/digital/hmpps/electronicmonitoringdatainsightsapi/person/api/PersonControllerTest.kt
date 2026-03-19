package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service.PersonService

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PersonController::class)
class PersonControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockkBean
  private lateinit var personService: PersonService

  @Test
  fun `getPerson should return 200 and person when they exist`() {
    // Arrange
    val personId = "123456"
    val mockPerson = Person(personId = "123456")

    every { personService.getPersonById(personId) } returns mockPerson

    // Act & Assert
    mockMvc.perform(
      get("/people/$personId")
        .accept(MediaType.APPLICATION_JSON),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.personId").value("123456"))

    verify(exactly = 1) { personService.getPersonById(personId) }
  }

  @Test
  fun `getPerson should return 404 when person not found`() {
    // Arrange
    val personId = "123456"
    every { personService.getPersonById(personId) } returns null

    // Act & Assert
    mockMvc.perform(
      get("/people/$personId").accept(MediaType.APPLICATION_JSON),
    )
      .andExpect(status().isNotFound)

    verify(exactly = 1) { personService.getPersonById(personId) }
  }
}
