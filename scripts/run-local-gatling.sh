#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
token_script="$script_dir/getEMDIDEVToken.sh"

simulation="uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.ExistsInEmdiSimulation"
base_url="${BASE_URL:-http://localhost:8080}"
auth_url="${AUTH_URL:-https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token}"

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

echo "Running Gatling simulation against $base_url"

BASE_URL="$base_url" \
AUTH_TOKEN="$auth_token" \
AUTH_URL="$auth_url" \
CLIENT_ID="${CLIENT_ID:-unused}" \
CLIENT_SECRET="${CLIENT_SECRET:-unused}" \
./gradlew gatlingRun --simulation "$simulation"
