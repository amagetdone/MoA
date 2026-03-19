# Security Scan GitLab CI Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a single GitLab CI security stage with separate Sonar SAST, dependency vulnerability scan, and secret scan jobs.

**Architecture:** The pipeline keeps one shared `security-scan` stage for speed, while each scanner remains an independent job with its own logs and pass/fail state. Shell-runner JDK bootstrapping is reused so Gradle-based checks work in the current GitLab environment.

**Tech Stack:** GitLab CI, Gradle Wrapper, SonarQube Gradle Plugin, OWASP Dependency-Check Gradle Plugin, POSIX shell script

---

## Chunk 1: Extend backend security tooling

### Task 1: Add dependency vulnerability scanning support

**Files:**
- Modify: `BE/amagetdone/build.gradle`

- [ ] **Step 1: Add the OWASP Dependency-Check Gradle plugin**

Insert:

```groovy
id 'org.owasp.dependencycheck' version '12.1.0'
```

- [ ] **Step 2: Add minimal dependency-check configuration**

Insert a `dependencyCheck { ... }` block that:
- sets the output directory under `build/reports`
- chooses HTML output
- keeps the configuration simple for the first rollout

- [ ] **Step 3: Verify Gradle registers the dependency-check tasks**

Run: `./gradlew tasks --all`
Expected: dependency-check tasks appear, or record that plugin resolution is blocked by network limits.

## Chunk 2: Split GitLab CI into separate security jobs

### Task 2: Replace the single Sonar job with one stage and three jobs

**Files:**
- Modify: `.gitlab-ci.yml`
- Create: `scripts/ci/secret_scan.sh`

- [ ] **Step 1: Rename the stage to `security-scan`**

Keep one stage:

```yaml
stages:
  - security-scan
```

- [ ] **Step 2: Create shared shell-runner setup**

Reuse the JDK 17 detection logic for all Gradle-based jobs.

- [ ] **Step 3: Add `sonar-sast`, `dependency-scan`, and `secret-scan` jobs**

Requirements:
- same branch/MR rules across all jobs
- `allow_failure: true` for the first rollout
- artifacts for reports where available

## Chunk 3: Verify the rollout

### Task 3: Run the checks that are possible locally

**Files:**
- Modify: `.gitlab-ci.yml`
- Modify: `BE/amagetdone/build.gradle`
- Create: `scripts/ci/secret_scan.sh`

- [ ] **Step 1: Run `./gradlew tasks --all` with JDK 17**

Run: `JAVA_HOME=/tmp/jdk17 PATH=/tmp/jdk17/bin:$PATH ./gradlew tasks --all`
Expected: `sonar` and dependency-check tasks are listed.

- [ ] **Step 2: Run the secret scan script**

Run: `bash scripts/ci/secret_scan.sh`
Expected: exit 0 when no risky secrets are found in tracked files.

- [ ] **Step 3: Summarize any verification gaps**

Record whether dependency data downloads or external credentials could not be fully verified locally.
