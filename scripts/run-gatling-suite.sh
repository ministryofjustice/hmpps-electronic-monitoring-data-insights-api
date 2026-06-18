#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
token_script="$script_dir/getEMDIDEVToken.sh"
base_url="${BASE_URL:-http://localhost:8080}"
auth_url="${AUTH_URL:-https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token}"
crn="${CRN:-X994316}"
person_id="${PERSON_ID:-10001}"

if [[ ! -f "$token_script" ]]; then
  echo "Token script not found: $token_script" >&2
  exit 1
fi

echo "Fetching EMDI dev token..."
token_output="$(bash "$token_script")"
auth_token="$(
  printf '%s\n' "$token_output" |
    awk '$0 ~ /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/ { token=$0 } END { print token }'
)"

if [[ -z "$auth_token" ]]; then
  echo "Failed to extract JWT from token script output." >&2
  echo "$token_output" >&2
  exit 1
fi

cd "$repo_root"

echo "Running all Gatling simulations in uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi against $base_url with CRN $crn and person ID $person_id"

BASE_URL="$base_url" \
AUTH_TOKEN="$auth_token" \
AUTH_URL="$auth_url" \
CLIENT_ID="${CLIENT_ID:-unused}" \
CLIENT_SECRET="${CLIENT_SECRET:-unused}" \
CRN="$crn" \
PERSON_ID="$person_id" \
./gradlew gatlingRun --all
