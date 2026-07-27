#!/usr/bin/env bash

set -euo pipefail

readonly NAMESPACE="hmpps-electronic-monitoring-data-insights-prod"
readonly SECRET_NAME="slack-webhook-prod"
readonly SECRET_KEY="hub_channel_url"

command -v kubectl >/dev/null 2>&1 || {
  echo "Error: kubectl is not installed or is not on PATH." >&2
  exit 1
}

context="$(kubectl config current-context)"

echo "Kubernetes context: ${context}"
echo "Namespace:          ${NAMESPACE}"
echo "Secret:             ${SECRET_NAME}"
echo

if kubectl get secret "${SECRET_NAME}" --namespace "${NAMESPACE}" >/dev/null 2>&1; then
  echo "Error: secret ${SECRET_NAME} already exists in ${NAMESPACE}." >&2
  echo "This script will not overwrite it." >&2
  exit 1
fi

read -r -s -p "Slack webhook URL: " slack_webhook_url
echo

if [[ ! "${slack_webhook_url}" =~ ^https://hooks\.slack\.com/services/ ]]; then
  echo "Error: the value does not look like a Slack webhook URL." >&2
  exit 1
fi

read -r -p "Create the production secret in this context? [y/N] " confirmation
if [[ ! "${confirmation}" =~ ^[Yy]$ ]]; then
  echo "Cancelled."
  exit 0
fi

kubectl create secret generic "${SECRET_NAME}" \
  --namespace "${NAMESPACE}" \
  --from-literal="${SECRET_KEY}=${slack_webhook_url}"

echo "Created ${SECRET_NAME} in ${NAMESPACE}."
