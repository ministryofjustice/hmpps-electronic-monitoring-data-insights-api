package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

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

  fun updateGreeting(id: UUID, newMessage: String): Greeting? = transaction {
    val updated = Greetings.update({ Greetings.id eq id }) {
      it[message] = newMessage
      it[updatedAt] = LocalDateTime.now()
    }

    if (updated > 0) {
      Greetings.selectAll().where { Greetings.id eq id }
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

  fun getGreeting(): Greeting? = transaction {
    Greetings.selectAll()
      .orderBy(
        Greetings.createdAt to SortOrder.DESC,
        Greetings.updatedAt to SortOrder.DESC,
        Greetings.id to SortOrder.DESC,
      )
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

  fun getGreetingById(id: UUID): Greeting? = transaction {
    Greetings
      .selectAll()
      .where { Greetings.id eq id }
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
