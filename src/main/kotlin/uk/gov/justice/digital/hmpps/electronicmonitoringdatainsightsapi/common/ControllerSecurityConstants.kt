package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common

// Role Constants
const val EDIT_ROLE = "ROLE_EM_DATA_INSIGHTS__RW"
const val VIEW_ROLE = "ROLE_EM_DATA_INSIGHTS__RO"

// Authority Checks
const val HAS_EDIT_ROLE = """hasAuthority('$EDIT_ROLE')"""

const val HAS_VIEW_ROLE = """hasAnyAuthority("$EDIT_ROLE", "$VIEW_ROLE")"""
