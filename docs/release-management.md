# Release management

The API is versioned and released independently from the UI. A release is an immutable version that has passed CI and
has been tested in development. Creating a release does not deploy it to pre-production or production.

Every push to `main` is built with a development snapshot version and deployed only to the development environment.
Once the current `main` commit has been tested, the API can be released without waiting for a UI release or UI sign-off.

## Create a release

Before starting, confirm that the current `main` commit is the exact commit tested in development and approved for
release. Then:

1. Open **Actions** and select **Create release**.
2. Select **Run workflow** on the `main` branch.
3. Choose the version increment and start the workflow.

The first release is always `v1.0.0`. For later releases choose:

| Increment | Use when the release contains                                 |
| --------- | ------------------------------------------------------------- |
| `major`   | An incompatible or breaking change                            |
| `minor`   | Backwards-compatible functionality                            |
| `patch`   | Backwards-compatible fixes, maintenance or dependency updates |

For a release containing several changes, select the largest applicable increment. Dependency-only releases are
normally `patch` unless an update introduces a breaking change.

The workflow serialises release attempts, verifies that the source commit completed the normal `main` pipeline and
development deployment, reruns the application and Helm tests, calculates the next version, and creates:

- an annotated Git tag named `vMAJOR.MINOR.PATCH`;
- a container image tagged `MAJOR.MINOR.PATCH`; and
- a GitHub release with notes generated from merged pull requests since the previous release.

The version without the leading `v` is built into the API. The deployed version is reported consistently by OpenAPI
`info.version` and Swagger UI, actuator/build information, and Application Insights `service.version` metadata.

Tags and released container versions are immutable. Do not delete, move or reuse them.

## Promote a release

Open **Actions**, select **Deploy to environment**, choose the environment and enter the released version without its
leading `v`, for example `1.2.3`. The workflow rejects malformed versions, draft or missing GitHub releases, and
missing container images before deployment. Environment approval rules continue to control promotion.

Promote the same released container image through every environment; do not rebuild it between environments.

## Failed releases and deployments

If release creation fails, rerun the same workflow run after correcting the operational problem. If its tag was already
created, the workflow verifies that it still points to the expected commit and resumes without creating a second tag.
A rerun of a completed release reports that it is already complete and does not recreate the tag, image or release.

If code must change, merge the fix, test it in development and create a new release. Never move an existing tag.

If deployment fails for an environmental reason, rerun **Deploy to environment** with the same version. To roll back,
run that workflow with the version of a previously successful release. Neither action creates a new release.
