package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.hello

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import com.fasterxml.jackson.databind.ObjectMapper

@WebMvcTest(HelloController::class)
class HelloControllerTest @Autowired constructor(
  val mockMvc: MockMvc,
  val objectMapper: ObjectMapper,
) {
  @MockBean
  lateinit var helloService: HelloService

  @Test
  fun `should store and retrieve hello value`() {
    val value = "test message"
    val request = HelloRequest(value)
    org.mockito.Mockito.doNothing().`when`(helloService).setValue(value)
    org.mockito.Mockito.`when`(helloService.getValue()).thenReturn(value)

    mockMvc.perform(
      post("/hello")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
    ).andExpect(status().isOk)

    mockMvc.perform(get("/hello"))
      .andExpect(status().isOk)
      .andExpect(content().json(objectMapper.writeValueAsString(HelloResponse(value))))
  }
}
