package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model

internal object TagStatus {
  fun decode(hdop: Int?): String? = when (hdop) {
    0 -> "No Tamper"
    1 -> "Exclusion breach"
    2 -> "Inclusion breach"
    3 -> "Exclusion breach and Inclusion breach"
    4 -> "Exclusion buffer breach"
    5 -> "Exclusion breach and Exclusion buffer breach"
    16 -> "PID tamper"
    17 -> "Exclusion breach and PID tamper"
    18 -> "Inclusion breach and PID tamper"
    20 -> "Exclusion buffer breach and PID tamper"
    64 -> "Unit tamper"
    65 -> "Exclusion breach and Unit tamper"
    80 -> "PID tamper and Unit tamper"
    81 -> "Exclusion breach and PID tamper and Unit tamper"
    128 -> "Unit battery low"
    129 -> "Exclusion breach and Unit battery low"
    130 -> "Inclusion breach and Unit battery low"
    131 -> "Exclusion breach and Inclusion breach and Unit battery low"
    132 -> "Exclusion buffer breach and Unit battery low"
    144 -> "PID tamper and Unit battery low"
    145 -> "Exclusion breach and PID tamper and Unit battery low"
    146 -> "Inclusion breach and PID tamper and Unit battery low"
    148 -> "Exclusion buffer breach and PID tamper and Unit battery low"
    192 -> "Unit tamper and Unit battery low"
    208 -> "PID tamper and Unit tamper and Unit battery low"
    else -> ""
  }
}
