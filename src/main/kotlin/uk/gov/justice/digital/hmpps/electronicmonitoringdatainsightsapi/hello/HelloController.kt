package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.hello
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/hello")
class HelloController(private val helloService: HelloService) {
  @PostMapping
  fun setHello(@RequestBody body: HelloRequest): ResponseEntity<Void> {
    helloService.setValue(body.value)
    return ResponseEntity.ok().build()
  }

  @GetMapping
  fun getHello(): ResponseEntity<HelloResponse> {
    val value = helloService.getValue()
    return ResponseEntity.ok(HelloResponse(value))
  }
}
