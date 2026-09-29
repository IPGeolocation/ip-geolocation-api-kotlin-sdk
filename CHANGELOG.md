# Changelog

## 1.0.1

- Updated Jackson (`jackson-databind`, `jackson-core`, `jackson-module-kotlin`) from 2.21.1 to 2.21.7 to fix security vulnerabilities. No API or behavior changes.

## 1.0.0

- First Kotlin SDK release for the IPGeolocation.io IP Location API.
- Added typed and raw support for:
  - `/v3/ipgeo`
  - `/v3/ipgeo-bulk`
- Added config and request validation, response metadata, JSON helpers, and typed exceptions.
- Added local unit tests, live integration tests, and live field-parity tests.
