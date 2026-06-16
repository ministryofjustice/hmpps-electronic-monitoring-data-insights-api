
#!/usr/bin/env bash

set -euo pipefail

b64_no_wrap() {
  if base64 --help 2>&1 | grep -q -- ' -w '; then
    base64 -w 0
  else
    base64 -b 0
  fi
}

NAMESPACE="hmpps-electronic-monitoring-data-insights-dev"
CLIENT_ID_KEY="CLIENT_CREDS_CLIENT_ID"
CLIENT_SECRET_KEY="CLIENT_CREDS_CLIENT_SECRET"
TOKEN_NAME="EMDI API Token"
SECRET="hmpps-electronic-monitoring-data-insights-ui-client-creds"
SERVICE_POD_NAME_PREFIX="hmpps-em-data-insights-dev-service-pod"

AUTH_URL="https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token?grant_type=client_credentials"

for cmd in kubectl jq curl base64 grep head; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "$cmd is required but not installed." >&2
    exit 1
  fi
done

echo "Fetching secret data from Kubernetes..."

AUTH_STRING=$(
  kubectl -n "$NAMESPACE" get secret "$SECRET" -o json \
    | jq -r --arg id "$CLIENT_ID_KEY" --arg secret "$CLIENT_SECRET_KEY" \
      '.data | map_values(@base64d) | "\(.[$id]):\(.[$secret])"'
)

if [[ -z "${AUTH_STRING}" || "${AUTH_STRING}" == ":" ]]; then
  echo "Failed to extract credentials from secret." >&2
  exit 1
fi

CLIENT_AUTH=$(printf %s "$AUTH_STRING" | b64_no_wrap)

echo "Finding service pod..."

POD=$(
  kubectl -n "$NAMESPACE" get pods \
    -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' \
    | grep "^${SERVICE_POD_NAME_PREFIX}-" \
    | head -n 1
)

if [[ -z "$POD" ]]; then
  echo "Failed to find service pod matching prefix: ${SERVICE_POD_NAME_PREFIX}-" >&2
  echo "Available pods in namespace $NAMESPACE:" >&2
  kubectl -n "$NAMESPACE" get pods >&2
  exit 1
fi

echo "Using pod: $POD"

echo "Requesting ${TOKEN_NAME} from HMPPS Auth via pod..."

RESPONSE=$(
  kubectl -n "$NAMESPACE" exec "$POD" -- \
    curl -sS -X POST "$AUTH_URL" \
      -H "Authorization: Basic $CLIENT_AUTH"
)

AUTH_TOKEN=$(printf '%s' "$RESPONSE" | jq -r '.access_token // empty')

if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "Failed to retrieve ${TOKEN_NAME}." >&2
  echo "Response was:" >&2
  echo "$RESPONSE" >&2
  exit 1
fi

echo
echo "-----------------------------"
echo "${AUTH_TOKEN}"
echo "-----------------------------"
echo

