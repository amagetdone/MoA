# SonarQube GitLab CI Integration Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a non-blocking SonarQube analysis pipeline for the Spring backend on merge requests and core backend branches.

**Architecture:** GitLab CI will own SonarQube execution, while the backend Gradle module exposes the Sonar task through the official plugin. The first rollout is intentionally isolated to one backend job so the team can validate the workflow before adding merge gates or more security scanners.

**Tech Stack:** GitLab CI, Gradle Wrapper, Spring Boot, SonarQube Gradle Plugin

---

## Chunk 1: Document and wire the backend Sonar task

### Task 1: Add SonarQube plugin and project metadata

**Files:**
- Modify: `BE/amagetdone/build.gradle`

- [ ] **Step 1: Add the SonarQube Gradle plugin to the plugins block**

Insert:

```groovy
id 'org.sonarqube' version '7.2.0.6526'
```

- [ ] **Step 2: Add SonarQube project properties near the end of the file**

Insert:

```groovy
sonar {
    properties {
        property 'sonar.projectKey', 'S14P21D101'
        property 'sonar.projectName', 'S14P21D101'
        property 'sonar.qualitygate.wait', true
    }
}
```

- [ ] **Step 3: Verify Gradle syntax remains valid**

Run: `./gradlew tasks --all`
Expected: Gradle lists tasks without Groovy syntax errors. If local JDK 17 is missing, note the environment limitation.

## Chunk 2: Add the first GitLab Sonar job

### Task 2: Create the repository GitLab CI file

**Files:**
- Create: `.gitlab-ci.yml`

- [ ] **Step 1: Declare the CI image, cache variables, and stage**

Add:

```yaml
image: gradle:8.10.0-jdk17-jammy

variables:
  SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
  GIT_DEPTH: "0"

stages:
  - build-sonar
```

- [ ] **Step 2: Add a backend Sonar job that runs from `BE/amagetdone`**

Add a `build-sonar` job that:
- creates and reuses the Sonar cache
- changes directory into `BE/amagetdone`
- makes `gradlew` executable
- runs `./gradlew sonar`
- stays non-blocking with `allow_failure: true`

- [ ] **Step 3: Restrict execution to the agreed events**

Add rules for:
- `merge_request_event`
- `main`
- `develop`
- `dev/backend`

## Chunk 3: Verify the rollout

### Task 3: Run local checks and capture any environment gaps

**Files:**
- Modify: `.gitlab-ci.yml`
- Modify: `BE/amagetdone/build.gradle`

- [ ] **Step 1: Run `./gradlew tasks --all` from `BE/amagetdone`**

Run: `./gradlew tasks --all`
Expected: Sonar tasks appear if JDK 17 is available locally.

- [ ] **Step 2: If possible, run `./gradlew sonar -Dsonar.host.url=...` only as a syntax smoke test**

Run: `./gradlew sonar -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.token=$SONAR_TOKEN`
Expected: Configuration is accepted up to the point allowed by available credentials and environment.

- [ ] **Step 3: Summarize any local verification limits before handoff**

Record whether verification was blocked by missing JDK 17, missing tokens, or restricted network access, then hand off to GitLab UI validation.
