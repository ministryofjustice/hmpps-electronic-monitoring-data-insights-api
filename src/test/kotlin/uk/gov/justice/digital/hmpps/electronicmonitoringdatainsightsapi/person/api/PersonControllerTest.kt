package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.api

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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonController
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
  fun `findByCrn should return 200 and list of people if they exist`() {
    // Arrange
    val crn = "123456"
    val mockPerson = listOf(
      Person(personId = "123456"),
    )

    // Act
    every { personService.findByCrn(crn) } returns mockPerson

    // Assert
    mockMvc.perform(
      get("/people/$crn")
        .accept(MediaType.APPLICATION_JSON),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].personId").value("123456"))

    verify(exactly = 1) { personService.findByCrn(crn) }
  }

  @Test
  fun `findByCrn should return 200 and empty list when no devices found`() {
    // Arrange
    val crn = "123456"

    // Act
    every { personService.findByCrn(crn) } returns emptyList()

    // Assert
    mockMvc.perform(get("/people/$crn"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(0))

    verify(exactly = 1) { personService.findByCrn(crn) }
  }
}
