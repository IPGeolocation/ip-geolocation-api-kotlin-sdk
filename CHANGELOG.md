# Changelog

## 1.0.2

- Hide API keys in error messages and details.
- Reject missing or invalid typed responses.
- Fix handling of very short and very long timeouts.
- Add Arabic (`ar`) for single and bulk lookups.
- Update Jackson to 2.21.8 and Maven build dependencies, and add Java 8 and Java 21 checks.

Existing public methods and Java 8+ support remain available.

## 1.0.1

- Updated Jackson (`jackson-databind`, `jackson-core`, `jackson-module-kotlin`) from 2.21.1 to 2.21.7 to fix security vulnerabilities. No API or behavior changes.

## 1.0.0

- First Kotlin SDK release for the IPGeolocation.io IP Location API.
- Added typed and raw support for:
  - `/v3/ipgeo`
  - `/v3/ipgeo-bulk`
- Added config and request validation, response metadata, JSON helpers, and typed exceptions.
- Added local unit tests, live integration tests, and live field-parity tests.
