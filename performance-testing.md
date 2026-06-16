# Performance Tests

The project has a Gatling smoke test for the `existsInEMDI` endpoint using CRN `X777777`.

Run it with an existing token:

```bash
BASE_URL=<url_of_service> AUTH_TOKEN='<jwt_token>' ./gradlew gatlingRun-uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.ExistsInEmdiSimulation
```

Or let Gatling fetch a client credentials token:

```bash
BASE_URL=<url_of_service> AUTH_URL=<auth_url/auth/oauth/token> CLIENT_ID=<client_id> CLIENT_SECRET='<client_secret>' ./gradlew gatlingRun-uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.ExistsInEmdiSimulation
```

For local dev with the `dev` profile and stub data enabled, use `BASE_URL=http://localhost:8080`.

After running the test, the Gatling report is written to `build/reports/gatling/`.
