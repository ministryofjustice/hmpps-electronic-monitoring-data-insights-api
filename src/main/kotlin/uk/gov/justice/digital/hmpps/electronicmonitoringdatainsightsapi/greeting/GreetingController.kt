package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/greeting")
class GreetingController(private val greetingService: GreetingService) {

  @PostMapping
  fun createGreeting(@RequestBody request: GreetingRequest): ResponseEntity<Greeting> {
    val greeting = greetingService.createGreeting(request.message)
    return ResponseEntity.status(201).body(greeting)
  }

  @PutMapping("/{id}")
  fun updateGreeting(@PathVariable id: UUID, @RequestBody request: GreetingRequest): ResponseEntity<Greeting> {
    val updated = greetingService.updateGreeting(id, request.message)
    return if (updated != null) {
      ResponseEntity.ok(updated)
    } else {
      ResponseEntity.notFound().build()
    }
  }

  @GetMapping
  fun getGreeting(): ResponseEntity<Greeting> {
    val greeting = greetingService.getGreeting()
    return if (greeting != null) {
      ResponseEntity.ok(greeting)
    } else {
      ResponseEntity.notFound().build()
    }
  }

  @GetMapping("/{id}")
  fun getGreetingById(@PathVariable id: UUID): ResponseEntity<Greeting> {
    val greeting = greetingService.getGreetingById(id)
    return if (greeting != null) {
      ResponseEntity.ok(greeting)
    } else {
      ResponseEntity.notFound().build()
    }
  }
}
