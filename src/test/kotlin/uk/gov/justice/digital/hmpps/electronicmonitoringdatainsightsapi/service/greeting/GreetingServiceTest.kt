package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.service.greeting

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.GreetingService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.Greetings
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.testutil.norm
import java.util.UUID

@ActiveProfiles("test")
class GreetingServiceTest {
  private lateinit var service: GreetingService

  @BeforeEach
  fun setUp() {
    Database.connect("jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;", "org.h2.Driver")

    transaction {
      SchemaUtils.create(Greetings)
    }
    service = GreetingService()
  }

  @AfterEach
  fun tearDown() {
    transaction {
      Greetings.deleteAll()
    }
  }

  @Nested
  @DisplayName("Create Greeting")
  inner class CreateGreeting {
    @Test
    fun `it should create a new Greeting`() {
      val message = "Hi there"
      val result = service.createGreeting(message)

      assertThat(result.id).isNotNull()
      assertThat(result.message).isEqualTo("Hi there")
      assertThat(result.createdAt).isNotNull()
      assertThat(result.updatedAt).isNotNull()
    }

    @Test
    fun `it should create a unique Greeting`() {
      val message1 = "Hi there"
      val message2 = "Hi there 2"

      val result = service.createGreeting(message1)
      val result2 = service.createGreeting(message2)

      assertThat(result.id).isNotEqualTo(result2.id)
    }
  }

  @Nested
  @DisplayName("Update Greeting")
  inner class UpdateGreeting {
    @Test
    fun `it should update a Greeting`() {
      val message = "original message"
      val message2 = "updated message"
      val result = service.createGreeting(message)
      val result2 = service.updateGreeting(result.id, message2)

      assertThat(result2?.id).isEqualTo(result.id)
      assertThat(result2?.message).isNotEqualTo(result.message)
      assertThat(result2?.createdAt?.norm()).isEqualTo(result.createdAt.norm())
      assertThat(result2?.updatedAt?.norm()).isNotEqualTo(result.updatedAt.norm())
    }

    @Test
    fun `it should return null for non-existent Greeting`() {
      val id = UUID.randomUUID()
      val message = "some message"
      val result = service.updateGreeting(id, message)

      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("Get Greeting")
  inner class GetGreeting {
    @Test
    fun `it should get a Greeting`() {
      val message = "some message"
      val message2 = "some message2"
      service.createGreeting(message)
      val latest = service.createGreeting(message2)

      val result = service.getGreeting()

      assertThat(result?.id).isEqualTo(latest.id)
      assertThat(result?.message).isEqualTo(latest.message)
      assertThat(result?.createdAt?.norm()).isEqualTo(latest.createdAt.norm())
      assertThat(result?.updatedAt?.norm()).isEqualTo(latest.updatedAt.norm())
    }
  }

  @Nested
  @DisplayName("Get Greeting By Id")
  inner class GetGreetingById {
    @Test
    fun `it should get a Greeting by Id`() {
      val message = "some message"
      val created = service.createGreeting(message)
      val result = service.getGreetingById(created.id)

      assertThat(result?.id).isEqualTo(created.id)
      assertThat(result?.message).isEqualTo(created.message)
      assertThat(result?.createdAt?.norm()).isEqualTo(created.createdAt.norm())
      assertThat(result?.updatedAt?.norm()).isEqualTo(created.updatedAt.norm())
    }
  }
}
