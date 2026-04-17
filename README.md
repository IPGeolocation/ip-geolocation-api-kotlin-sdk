# IPGeolocation Kotlin SDK

Official Kotlin SDK for the IPGeolocation.io IP Location API.

Look up IPv4, IPv6, and domains with `/v3/ipgeo` and `/v3/ipgeo-bulk`. Get geolocation, company, ASN, timezone, network, hostname, abuse, user-agent, currency, and security data from one API.

- Kotlin/JVM SDK targeting Java 8+
- Typed JSON responses plus raw JSON and XML methods
- Built on the JDK HTTP stack

## Table of Contents

- [Install](#install)
- [Quick Start](#quick-start)
- [At a Glance](#at-a-glance)
- [Get Your API Key](#get-your-api-key)
- [Authentication](#authentication)
- [Plan Behavior](#plan-behavior)
- [Client Configuration](#client-configuration)
- [Available Methods](#available-methods)
- [Request Options](#request-options)
- [Examples](#examples)
- [Response Metadata](#response-metadata)
- [JSON Helpers](#json-helpers)
- [Errors](#errors)
- [Troubleshooting](#troubleshooting)
- [Frequently Asked Questions](#frequently-asked-questions)
- [Links](#links)

## Install

Maven:

```xml
<dependency>
  <groupId>io.ipgeolocation</groupId>
  <artifactId>ipgeolocation-kotlin-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle Kotlin DSL:

```kotlin
implementation("io.ipgeolocation:ipgeolocation-kotlin-sdk:1.0.0")
```

Gradle Groovy:

```groovy
implementation 'io.ipgeolocation:ipgeolocation-kotlin-sdk:1.0.0'
```

Maven Central coordinates: `io.ipgeolocation:ipgeolocation-kotlin-sdk`
Package page: <https://central.sonatype.com/artifact/io.ipgeolocation/ipgeolocation-kotlin-sdk>
GitHub repository: <https://github.com/IPGeolocation/ip-geolocation-api-kotlin-sdk>

All public types live under the `io.ipgeolocation.sdk` package. Your IDE will auto-import the classes used in the examples below.

### Building from source

If you want to build the SDK locally instead of pulling it from Maven Central, clone the repository and install it into your local Maven cache:

```bash
git clone https://github.com/IPGeolocation/ip-geolocation-api-kotlin-sdk.git
cd ip-geolocation-api-kotlin-sdk
mvn install
```

## Quick Start

```kotlin
import io.ipgeolocation.sdk.IpGeolocationClient
import io.ipgeolocation.sdk.IpGeolocationClientConfig
import io.ipgeolocation.sdk.LookupIpGeolocationRequest

fun main() {
    val apiKey = System.getenv("IPGEO_API_KEY")
        ?: error("set IPGEO_API_KEY first")

    IpGeolocationClient(
        IpGeolocationClientConfig(apiKey = apiKey),
    ).use { client ->
        val response = client.lookupIpGeolocation(
            LookupIpGeolocationRequest(ip = "8.8.8.8"),
        )

        println(response.data.ip) // 8.8.8.8
        println(response.data.location?.countryName) // United States
        println(response.data.location?.city)
        println(response.data.timeZone?.name)
        println(response.metadata.creditsCharged)
    }
}
```

You can also call `lookupIpGeolocation()` with no request object to resolve the caller IP.

## At a Glance

| Item | Value |
|------|-------|
| Artifact | `io.ipgeolocation:ipgeolocation-kotlin-sdk` |
| Package | `io.ipgeolocation.sdk` |
| Supported Endpoints | `/v3/ipgeo`, `/v3/ipgeo-bulk` |
| Supported Inputs | IPv4, IPv6, domain |
| Main Data Returned | Geolocation, company, ASN, timezone, network, hostname, abuse, user-agent, currency, security |
| Authentication | API key, request-origin auth for `/v3/ipgeo` only |
| Response Formats | Typed JSON, raw JSON, raw XML |
| Bulk Limit | Up to 50,000 IPs or domains per request |
| Transport | JDK `HttpURLConnection` |

## Get Your API Key

Create an IPGeolocation account and copy an API key from your dashboard.

1. Sign up: <https://app.ipgeolocation.io/signup>
2. Verify your email if prompted
3. Sign in: <https://app.ipgeolocation.io/login>
4. Open your dashboard: <https://app.ipgeolocation.io/dashboard>
5. Copy an API key from the `API Keys` section

For server-side Kotlin code, keep the API key in an environment variable or secret manager. For browser-based single lookups on paid plans, use request-origin auth instead of exposing an API key in frontend code.

## Authentication

### API Key

```kotlin
val client = IpGeolocationClient(
    IpGeolocationClientConfig(
        apiKey = System.getenv("IPGEO_API_KEY"),
    ),
)
```

### Request-Origin Auth

```kotlin
val client = IpGeolocationClient(
    IpGeolocationClientConfig(
        requestOrigin = "https://app.example.com",
    ),
)
```

`requestOrigin` must be an absolute `http` or `https` origin with no path, query string, fragment, or userinfo.

> [!IMPORTANT]
> Request-origin auth does not work with `/v3/ipgeo-bulk`. Bulk lookup always requires `apiKey`.

> [!NOTE]
> If you set both `apiKey` and `requestOrigin`, single lookup still uses the API key. The API key is sent as the `apiKey` query parameter, so avoid logging full request URLs.

## Plan Behavior

Feature availability depends on your plan and request parameters.

| Capability | Free | Paid |
|------------|------|------|
| Single IPv4 and IPv6 lookup | Supported | Supported |
| Domain lookup | Not supported | Supported |
| Bulk lookup | Not supported | Supported |
| Non-English `lang` | Not supported | Supported |
| Request-origin auth | Not supported | Supported for `/v3/ipgeo` only |
| Optional modules via `include` | Not supported | Supported |
| `include = listOf("*")` | Base response only | All plan-available modules |

Paid plans still need `include` for optional modules. `fields` and `excludes` only trim the response. They do not turn modules on or unlock paid data.

## Client Configuration

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `apiKey` | `String?` | unset | Required for bulk lookup. Optional for single lookup if `requestOrigin` is set. |
| `requestOrigin` | `String?` | unset | Must be an absolute `http` or `https` origin with no path, query string, fragment, or userinfo. |
| `baseUrl` | `String` | `https://api.ipgeolocation.io` | Override the API base URL. |
| `connectTimeout` | `Duration` | `Duration.ofSeconds(10)` | Time to open the connection. Must be greater than zero and must be less than or equal to `readTimeout`. |
| `readTimeout` | `Duration` | `Duration.ofSeconds(30)` | Time to wait while reading the response body. Must be greater than zero and must be greater than or equal to `connectTimeout`. |
| `maxResponseBodyChars` | `Int` | `33554432` | Maximum response body size the SDK will buffer before failing the request. Must be greater than zero. |

Config values are validated when the client is created. Request values are validated before each request is sent.

## Available Methods

| Method | Returns | Notes |
|--------|---------|-------|
| `lookupIpGeolocation(request = LookupIpGeolocationRequest())` | `ApiResponse<IpGeolocationResponse>` | Single lookup. Typed JSON response. |
| `lookupIpGeolocationRaw(request = LookupIpGeolocationRequest())` | `ApiResponse<String>` | Single lookup. Raw JSON or XML string. |
| `bulkLookupIpGeolocation(request)` | `ApiResponse<List<BulkLookupResult>>` | Bulk lookup. Typed JSON response. |
| `bulkLookupIpGeolocationRaw(request)` | `ApiResponse<String>` | Bulk lookup. Raw JSON or XML string. |
| `close()` | `Unit` | Closes the client. Closed clients cannot be reused. |
| `defaultUserAgent()` | `String` | Returns the SDK default outbound `User-Agent` header value. |

> [!NOTE]
> Typed methods support JSON only. Use the raw methods when you need XML output.

## Request Options

| Field | Applies To | Notes |
|-------|------------|-------|
| `ip` | Single lookup | IPv4, IPv6, or domain. Leave it empty for caller IP lookup. |
| `ips` | Bulk lookup | List of 1 to 50,000 IPs or domains. |
| `lang` | Single and bulk | `Language.EN`, `DE`, `RU`, `JA`, `FR`, `CN`, `ES`, `CS`, `IT`, `KO`, `FA`, or `PT`. |
| `include` | Single and bulk | List of module names such as `security`, `abuse`, `user_agent`, `hostname`, `liveHostname`, `hostnameFallbackLive`, `geo_accuracy`, `dma_code`, or `*`. |
| `fields` | Single and bulk | List of field paths to keep, for example `location.country_name` or `security.threat_score`. |
| `excludes` | Single and bulk | List of field paths to remove from the response. |
| `userAgent` | Single and bulk | Overrides the outbound `User-Agent` header. |
| `headers` | Single and bulk | Extra request headers as `Map<String, String>`. |
| `output` | Single and bulk | `ResponseFormat.JSON` or `ResponseFormat.XML`. Typed methods require JSON. |

## Examples

The examples below assume you already have a configured client in scope:

```kotlin
val client = IpGeolocationClient(
    IpGeolocationClientConfig(
        apiKey = System.getenv("IPGEO_API_KEY"),
    ),
)
```

### Caller IP

Leave `ip` empty to look up the public IP of the machine making the request.

```kotlin
val response = client.lookupIpGeolocation()

println(response.data.ip)
```

### Domain Lookup

Domain lookup is a paid-plan feature.

```kotlin
val response = client.lookupIpGeolocation(
    LookupIpGeolocationRequest(
        ip = "ipgeolocation.io",
    ),
)

println(response.data.ip)
println(response.data.domain) // ipgeolocation.io
println(response.data.location?.countryName)
```

### Security and Abuse

```kotlin
val response = client.lookupIpGeolocation(
    LookupIpGeolocationRequest(
        ip = "9.9.9.9",
        include = listOf("security", "abuse"),
    ),
)

println(response.data.security?.threatScore)
println(response.data.abuse?.emails?.firstOrNull())
```

### User-Agent Parsing

To parse a visitor user-agent string, pass `include = listOf("user_agent")` and send the visitor string in the request `User-Agent` header.

```kotlin
val visitorUserAgent =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/601.3.9 (KHTML, like Gecko) Version/9.0.2 Safari/601.3.9"

val response = client.lookupIpGeolocation(
    LookupIpGeolocationRequest(
        ip = "115.240.90.163",
        include = listOf("user_agent"),
        headers = mapOf("User-Agent" to visitorUserAgent),
    ),
)

println(response.data.userAgent?.name)
println(response.data.userAgent?.operatingSystem?.name)
```

> [!NOTE]
> The `userAgent` request field overrides the SDK's default outbound `User-Agent` header. It takes precedence over `headers["User-Agent"]`.
> `response.data.userAgent` is different. That field is the parsed visitor user-agent data returned by the API.

### Filtered Response

```kotlin
val response = client.lookupIpGeolocation(
    LookupIpGeolocationRequest(
        ip = "8.8.8.8",
        include = listOf("security"),
        fields = listOf(
            "location.country_name",
            "security.threat_score",
            "security.is_vpn",
        ),
        excludes = listOf("currency"),
    ),
)

println(response.data.location?.countryName)
println(response.data.security?.isVpn)
println(response.data.currency == null)
```

### Raw XML

```kotlin
val response = client.lookupIpGeolocationRaw(
    LookupIpGeolocationRequest(
        ip = "8.8.8.8",
        output = ResponseFormat.XML,
    ),
)

println(response.data)
```

### Bulk Lookup

```kotlin
val response = client.bulkLookupIpGeolocation(
    BulkLookupIpGeolocationRequest(
        ips = listOf("8.8.8.8", "1.1.1.1"),
    ),
)

for (result in response.data) {
    if (result.isSuccess()) {
        println(result.data?.ip)
    } else {
        println(result.error?.message)
    }
}
```

### Raw Bulk JSON

```kotlin
val response = client.bulkLookupIpGeolocationRaw(
    BulkLookupIpGeolocationRequest(
        ips = listOf("8.8.8.8", "1.1.1.1"),
    ),
)

println(response.data)
```

## Response Metadata

Every method returns `ApiResponse<T>`, where:

- `data` contains the typed object or raw response string
- `metadata` contains response details such as:
  - `creditsCharged`
  - `successfulRecords`
  - `statusCode`
  - `durationMs`
  - `rawHeaders`

Example:

```kotlin
println(response.metadata.statusCode)
println(response.metadata.durationMs)
println(response.metadata.firstHeaderValue("content-type"))
```

## JSON Helpers

Use `JsonOutput` for logs, debugging, or CLI output.

| Method | Notes |
|--------|-------|
| `JsonOutput.toJson(value)` | Compact JSON. Omits `null` fields. |
| `JsonOutput.toJson(value, JsonOutputMode.FULL)` | Includes `null` fields. |
| `JsonOutput.toPrettyJson(value)` | Pretty-printed compact JSON. |
| `JsonOutput.toPrettyJson(value, JsonOutputMode.FULL)` | Pretty-printed full JSON. |

Example:

```kotlin
val compact = JsonOutput.toPrettyJson(response.data)
val full = JsonOutput.toPrettyJson(response.data, JsonOutputMode.FULL)

println(compact)
println(full)
```

## Errors

The SDK throws typed exceptions instead of leaving you to parse error bodies yourself.

| Error Type | When it happens |
|------------|-----------------|
| `ValidationException` | Invalid config, invalid request values, or typed XML request |
| `RequestTimeoutException` | Connect timeout or read timeout |
| `TransportException` | Network or transport failure |
| `SerializationException` | Request or response serialization failure |
| `ApiException` | API returned a non-2xx response |

`ApiException` has status-specific subclasses:

- `BadRequestException`
- `UnauthorizedException`
- `NotFoundException`
- `MethodNotAllowedException`
- `PayloadTooLargeException`
- `UnsupportedMediaTypeException`
- `LockedException`
- `RateLimitException`
- `ClientClosedRequestException`
- `ServerErrorException`

`ApiException` also exposes:

- `statusCode`
- `apiMessage`

Example:

```kotlin
try {
    client.lookupIpGeolocation(
        LookupIpGeolocationRequest(output = ResponseFormat.XML),
    )
} catch (error: ValidationException) {
    println(error.message)
}
```

## Troubleshooting

- Bulk lookup always requires `apiKey`. `requestOrigin` is not enough.
- Typed methods only support JSON. Use the raw methods for XML.
- If you need security, abuse, user-agent, or hostname data, include those modules explicitly.
- `fields` and `excludes` filter the response. They do not unlock paid data.
- `requestOrigin` must be an origin only. Do not include a path, query string, fragment, or userinfo.
- Response fields are nullable so omitted fields stay omitted.
- `connectTimeout` must be less than or equal to `readTimeout`.
- The SDK rejects responses larger than `maxResponseBodyChars`. The default cap is `32 MiB`.
- If Maven prints `sun.misc.Unsafe` warnings from `kotlin-compiler` during local builds, run Maven with JDK 21 LTS. That warning is from the current Kotlin compiler toolchain, not from the SDK runtime.

## Frequently Asked Questions

<details>
<summary>Can I use this SDK without an API key?</summary>

Only for single lookup with paid-plan request-origin auth. Bulk lookup always requires an API key.
</details>

<details>
<summary>Can I request XML and still get typed models?</summary>

No. Typed methods only support JSON. Use `lookupIpGeolocationRaw` or `bulkLookupIpGeolocationRaw` for XML.
</details>

<details>
<summary>Why are many response fields nullable?</summary>

Nullable fields let the SDK preserve omitted fields instead of inventing default values for data the API did not send.
</details>

<details>
<summary>Does domain lookup work on the free plan?</summary>

No. Domain lookup is a paid-plan feature.
</details>

## Links

- Homepage: <https://ipgeolocation.io>
- IP Location API product page: <https://ipgeolocation.io/ip-location-api.html>
- Documentation: <https://ipgeolocation.io/documentation/ip-location-api.html>
- GitHub repository: <https://github.com/IPGeolocation/ip-geolocation-api-kotlin-sdk>
