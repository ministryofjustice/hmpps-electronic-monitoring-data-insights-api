package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.hello
import org.springframework.stereotype.Service

@Service
class HelloService {
    @Volatile private var value: String = ""

    fun setValue(newValue: String) {
        value = newValue
    }

    fun getValue(): String = value
}