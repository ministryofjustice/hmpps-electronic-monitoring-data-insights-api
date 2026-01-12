#!/bin/bash

NAMESPACE="${NAMESPACE:-hmpps-electronic-monitoring-data-insights-dev}"
LABEL="${LABEL:-hmpps-em-data-insights-dev-service-pod}"
PARAM_NAME=${PARAM_NAME:-athena_general_role_arn}

POD=$(kubectl \
  get pods \
  -l "name=${LABEL}" \
  -n ${NAMESPACE} \
  -o json)

POD_NAME=$(echo ${POD} | jq -r '.items[0].metadata.name')

echo "Found service pod with name: ${POD_NAME}"

CALLER=$(kubectl \
  exec -i --tty=false ${POD_NAME} \
  -n ${NAMESPACE} -- \
  bash -c "aws sts get-caller-identity")

ACCOUNT=$(echo ${CALLER} | jq -r '.Account')

PARAM_ARN="arn:aws:ssm:eu-west-2:${ACCOUNT}:parameter/${NAMESPACE}/${PARAM_NAME}"
PARAM=$(kubectl \
  exec -i --tty=false ${POD_NAME} \
  -n ${NAMESPACE} -- \
  bash -c "aws ssm get-parameter --name ${PARAM_ARN} --with-decryption")

ROLE=$(echo ${PARAM} | jq -r '.Parameter.Value')

echo "Athena role: ${ROLE}"