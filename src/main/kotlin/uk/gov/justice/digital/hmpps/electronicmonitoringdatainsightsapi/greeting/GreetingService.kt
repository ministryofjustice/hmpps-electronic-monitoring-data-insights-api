package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service

@Service
class GreetingService {

  private var greeting: Greeting? = null

  fun createGreeting(message: String): Greeting {
    val now = LocalDateTime.now()
    lateinit var generatedId: UUID

    transaction {
      generatedId = Greetings.insert {
        it[Greetings.message] = message
        it[createdAt] = now
        it[updatedAt] = now
      } get Greetings.id
    }

    return Greeting(
      id = generatedId,
      message = message,
      createdAt = now,
      updatedAt = now,
    )
  }

  fun updateGreeting(id: UUID, newMessage: String): Greeting? {
    return transaction {
      val updated = Greetings.update({ Greetings.id eq id }) {
        it[message] = newMessage
        it[updatedAt] = LocalDateTime.now()
      }

      if (updated > 0) {
        Greetings.select { Greetings.id eq id }
          .map {
            Greeting(
              id = it[Greetings.id],
              message = it[Greetings.message],
              createdAt = it[Greetings.createdAt],
              updatedAt = it[Greetings.updatedAt],
            )
          }.firstOrNull()
      } else {
        null
      }
    }
  }

  fun getGreeting(): Greeting? {
    return transaction {
      Greetings.selectAll()
        .orderBy(Greetings.id, SortOrder.DESC)
        .limit(1)
        .map {
          Greeting(
            id = it[Greetings.id],
            message = it[Greetings.message],
            createdAt = it[Greetings.createdAt],
            updatedAt = it[Greetings.updatedAt],
          )
        }.firstOrNull()
    }
  }

  fun getGreetingById(id: UUID): Greeting? {
    return transaction {
      Greetings.select { Greetings.id eq id }
        .map {
          Greeting(
            id = it[Greetings.id],
            message = it[Greetings.message],
            createdAt = it[Greetings.createdAt],
            updatedAt = it[Greetings.updatedAt],
          )
        }
        .firstOrNull()
    }
  }
}
