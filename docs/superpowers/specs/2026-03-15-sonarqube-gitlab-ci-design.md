# SonarQube GitLab CI Integration Design

## Goal
Add a first-pass SonarQube analysis flow for the Spring backend so the team can view analysis results from GitLab-driven work without enforcing merge blocking yet.

## Scope
- Target repository: `S14P21D101`
- Target application: `BE/amagetdone` Spring Boot Gradle project
- Execution targets: merge requests plus pushes to `main`, `develop`, and `dev/backend`
- Initial behavior: non-blocking (`allow_failure: true`)

## Decision Summary
- Use GitLab CI for SonarQube execution instead of Jenkins.
- Keep Jenkins focused on existing build and deployment flows.
- Create a repository-root `.gitlab-ci.yml` dedicated to the first SonarQube job.
- Run Sonar from the backend module directory because the Gradle wrapper lives in `BE/amagetdone`.

## Why GitLab CI
- Merge requests are the place where the team will naturally inspect quality results.
- GitLab CI makes it easier to evolve later into an MR quality gate.
- The repository does not currently have a GitLab pipeline file, so adding a focused Sonar job is low-risk.

## Pipeline Shape
- Use a single `build-sonar` stage.
- Use a JDK 17 Gradle image so the backend wrapper can run in CI.
- Set `SONAR_USER_HOME` to a project-local cache directory.
- Set `GIT_DEPTH` to `0` so SonarQube has full history metadata when needed.
- Cache the Sonar home directory.
- Execute `./gradlew sonar` from `BE/amagetdone`.

## Gradle Configuration
- Add the `org.sonarqube` plugin to `BE/amagetdone/build.gradle`.
- Add a `sonar { properties { ... } }` block with:
  - `sonar.projectKey = S14P21D101`
  - `sonar.projectName = S14P21D101`
  - `sonar.qualitygate.wait = true`

## Branch and Event Rules
- Run on `merge_request_event`
- Run on branch pushes to `main`
- Run on branch pushes to `develop`
- Run on branch pushes to `dev/backend`

## Risks and Follow-Up
- Local verification may be limited if a JDK 17 toolchain is unavailable outside CI.
- `allow_failure: true` means the pipeline will not block merges yet; this is intentional for the learning phase.
- Later phases can add dependency scanning, secret scanning, and a stricter MR gate.
