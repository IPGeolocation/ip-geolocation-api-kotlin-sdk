# Contributing Guide

This repository contains the public Kotlin SDK package for the IPGeolocation.io IP Location API.

## Prerequisites

- Java 8+
- Maven 3.8+

For local development and release verification, use JDK 21 LTS if it is available.
If Maven runs on JDK 25 with Kotlin `2.1.21`, you may see `sun.misc.Unsafe` warnings from the Kotlin compiler jars.
That warning is coming from the current Kotlin toolchain, not from this SDK code.

## Setup

```bash
mvn clean compile
```

If Maven is picking a newer JDK and printing Kotlin compiler `Unsafe` warnings, point Maven at JDK 21 before running the build:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean compile
```

## Run Tests

Run full public test suite:

```bash
mvn clean verify
```

Run all tests without cleaning:

```bash
mvn test
```

Run one test class:

```bash
mvn -Dtest=IpGeolocationClientParsingTest test
```

Run one test method:

```bash
mvn -Dtest=IpGeolocationClientParsingTest#parsesSingleLookupResponseAndMetadata test
```

## Live API Tests (Optional)

Live tests consume API credits and are disabled by default.

```bash
IPGEO_RUN_LIVE_TESTS=true \
IPGEO_FREE_KEY=... \
IPGEO_PAID_KEY=... \
mvn -Dtest=IpGeolocationLiveIntegrationTest test
```

Optional live hardening tests compare raw API JSON to the typed SDK model and consume additional credits.

```bash
IPGEO_RUN_LIVE_HARDENING=true \
IPGEO_PAID_KEY=... \
mvn -Dtest=IpGeolocationLiveFieldParityTest test
```

## Release Dry Run

Build the release artifacts without signing:

```bash
mvn -Prelease -Dgpg.skip=true clean verify
```

## Coding and PR Notes

- Keep changes focused and include tests for behavior changes.
- Do not commit real API keys, tokens, or secrets.
- Use clear commit messages, for example:
  - `feat: add raw bulk xml support`
  - `fix: preserve nested error.message in bulk results`
  - `test: add live parity coverage for user_agent fields`
