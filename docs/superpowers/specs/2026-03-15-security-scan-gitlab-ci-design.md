# Security Scan GitLab CI Design

## Goal
Expand the first GitLab CI security integration into a single `security-scan` stage with three pass/fail jobs that are easy to read in GitLab.

## Scope
- Target repository: `S14P21D101`
- Target backend module: `BE/amagetdone`
- Execution targets: merge requests plus pushes to `main`, `develop`, and `dev/backend`
- Initial rollout: non-blocking (`allow_failure: true`) for all jobs

## Decision Summary
- Keep one stage for speed and UI simplicity.
- Split checks into three independent jobs:
  - `sonar-sast`
  - `dependency-scan`
  - `secret-scan`
- Reuse the shell-runner JDK 17 detection logic because GitLab is using a shell executor.

## Why One Stage
- Jobs remain visually distinct in GitLab because each job has its own pass/fail result.
- The runner can execute jobs in parallel when capacity exists.
- The pipeline stays simpler than a three-stage sequential flow.

## Job Design

### `sonar-sast`
- Runs `./gradlew sonar` from `BE/amagetdone`
- Keeps current SonarQube integration

### `dependency-scan`
- Runs `./gradlew dependencyCheckAnalyze` from `BE/amagetdone`
- Uses the OWASP Dependency-Check Gradle plugin
- Publishes the generated HTML report as a GitLab artifact

### `secret-scan`
- Runs a repository-local shell script
- Checks tracked files for a small set of high-risk secret patterns
- Excludes obvious example files and generated docs to reduce noise
- Fails when any finding is detected

## Risks and Trade-Offs
- `dependency-scan` may be slower because it resolves vulnerability data.
- The first `secret-scan` is heuristic, not a full replacement for tools like Gitleaks.
- Since jobs are non-blocking for now, failures are visible without stopping merges.
