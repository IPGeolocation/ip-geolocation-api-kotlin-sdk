package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.internal.HttpRequestData
import io.ipgeolocation.sdk.internal.HttpResponseData
import io.ipgeolocation.sdk.model.Abuse
import io.ipgeolocation.sdk.model.Asn
import io.ipgeolocation.sdk.model.BulkLookupError
import io.ipgeolocation.sdk.model.BulkLookupResult
import io.ipgeolocation.sdk.model.Company
import io.ipgeolocation.sdk.model.CountryMetadata
import io.ipgeolocation.sdk.model.Currency
import io.ipgeolocation.sdk.model.DstTransition
import io.ipgeolocation.sdk.model.IpGeolocationResponse
import io.ipgeolocation.sdk.model.Location
import io.ipgeolocation.sdk.model.Network
import io.ipgeolocation.sdk.model.Security
import io.ipgeolocation.sdk.model.TimeZoneInfo
import io.ipgeolocation.sdk.model.UserAgent
import io.ipgeolocation.sdk.model.UserAgentDevice
import io.ipgeolocation.sdk.model.UserAgentEngine
import io.ipgeolocation.sdk.model.UserAgentOperatingSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.net.URI
import java.time.Duration

class ValueObjectCoverageTest {
    @Test
    fun dataClassesSupportEqualityHashCodeCopyAndToString() {
        val device = UserAgentDevice(name = "Mac", type = "desktop", brand = "Apple", cpu = "arm64")
        val engine = UserAgentEngine(name = "WebKit", type = "browser", version = "617.1", versionMajor = "617")
        val operatingSystem = UserAgentOperatingSystem(name = "macOS", type = "desktop", version = "14.0", versionMajor = "14", build = "23A344")
        val userAgent = UserAgent(
            userAgentString = "Mozilla/5.0",
            name = "Safari",
            type = "browser",
            version = "17.0",
            versionMajor = "17",
            device = device,
            engine = engine,
            operatingSystem = operatingSystem,
        )
        val location = Location(
            continentCode = "NA",
            continentName = "North America",
            countryCode2 = "US",
            countryCode3 = "USA",
            countryName = "United States",
            countryNameOfficial = "United States of America",
            countryCapital = "Washington",
            stateProv = "California",
            stateCode = "CA",
            district = "Santa Clara",
            city = "Mountain View",
            locality = "North Bayshore",
            accuracyRadius = "1000",
            confidence = "high",
            dmaCode = "807",
            zipcode = "94043",
            latitude = "37.386",
            longitude = "-122.0838",
            isEu = false,
            countryFlag = "https://example.com/flag.png",
            geonameId = "5375481",
            countryEmoji = "US",
        )
        val response = IpGeolocationResponse(
            ip = "8.8.8.8",
            hostname = "dns.google",
            domain = "google.com",
            location = location,
            countryMetadata = CountryMetadata(callingCode = "+1", tld = ".us", languages = listOf("en-US")),
            currency = Currency(code = "USD", name = "US Dollar", symbol = "$"),
            network = Network(connectionType = "wired", route = "8.8.8.0/24", isAnycast = true),
            asn = Asn(asNumber = "AS15169", organization = "Google", country = "US", type = "hosting", domain = "google.com", dateAllocated = "2000-01-01", rir = "ARIN"),
            company = Company(name = "Google", type = "public", domain = "google.com"),
            timeZone = TimeZoneInfo(
                name = "America/Los_Angeles",
                offset = -8.0,
                offsetWithDst = -7.0,
                currentTime = "2026-04-16 12:34:56",
                currentTimeUnix = 1_234_567_890.0,
                currentTzAbbreviation = "PDT",
                currentTzFullName = "Pacific Daylight Time",
                standardTzAbbreviation = "PST",
                standardTzFullName = "Pacific Standard Time",
                isDst = true,
                dstSavings = 1.0,
                dstExists = true,
                dstTzAbbreviation = "PDT",
                dstTzFullName = "Pacific Daylight Time",
                dstStart = DstTransition(
                    utcTime = "2026-03-08 10:00",
                    duration = "PT1H",
                    gap = true,
                    dateTimeAfter = "2026-03-08T03:00:00",
                    dateTimeBefore = "2026-03-08T01:59:59",
                    overlap = false,
                ),
                dstEnd = DstTransition(
                    utcTime = "2026-11-01 09:00",
                    duration = "PT1H",
                    gap = false,
                    dateTimeAfter = "2026-11-01T01:00:00",
                    dateTimeBefore = "2026-11-01T01:59:59",
                    overlap = true,
                ),
            ),
            security = Security(
                threatScore = 9.5,
                isTor = false,
                isProxy = true,
                proxyProviderNames = listOf("ExampleProxy"),
                proxyConfidenceScore = 0.8,
                proxyLastSeen = "2026-04-01",
                isResidentialProxy = false,
                isVpn = true,
                vpnProviderNames = listOf("ExampleVPN"),
                vpnConfidenceScore = 0.9,
                vpnLastSeen = "2026-04-01",
                isRelay = false,
                relayProviderName = null,
                isAnonymous = true,
                isKnownAttacker = false,
                isBot = false,
                isSpam = false,
                isCloudProvider = true,
                cloudProviderName = "ExampleCloud",
            ),
            userAgent = userAgent,
            abuse = Abuse(
                route = "8.8.8.0/24",
                country = "US",
                name = "Abuse Desk",
                organization = "Example Org",
                kind = "abuse",
                address = "1600 Amphitheatre Parkway",
                emails = listOf("abuse@example.com"),
                phoneNumbers = listOf("+1-555-0100"),
            ),
        )
        val success = BulkLookupResult(data = response)
        val error = BulkLookupResult(error = BulkLookupError("invalid ip"))
        val apiResponse = ApiResponse(
            data = response,
            metadata = ApiResponseMetadata(statusCode = 200, durationMs = 12),
        )
        val requestData = HttpRequestData(
            url = URI.create("https://api.ipgeolocation.io/v3/ipgeo"),
            method = "GET",
            headers = headers("X-Test", "value"),
            timeout = Duration.ofSeconds(3),
        )
        val responseData = HttpResponseData(
            statusCode = 200,
            body = """{"ok":true}""",
            headers = headers("X-Test", "value"),
        )
        val lookupRequest = LookupIpGeolocationRequest(
            ip = "8.8.8.8",
            lang = Language.EN,
            include = listOf("security"),
            fields = listOf("location.country_name"),
            excludes = listOf("currency"),
            userAgent = "TestAgent/1.0",
            headers = mapOf("X-Test" to "value"),
            output = ResponseFormat.JSON,
        )
        val bulkRequest = BulkLookupIpGeolocationRequest(
            ips = listOf("8.8.8.8", "1.1.1.1"),
            lang = Language.EN,
            include = listOf("security"),
            fields = listOf("location.country_name"),
            excludes = listOf("currency"),
            userAgent = "TestAgent/1.0",
            headers = mapOf("X-Test" to "value"),
            output = ResponseFormat.JSON,
        )

        val covered = listOf(
            device,
            engine,
            operatingSystem,
            userAgent,
            location,
            response.countryMetadata,
            response.currency,
            response.network,
            response.asn,
            response.company,
            response.timeZone,
            response.security,
            response.abuse,
            response.timeZone?.dstStart,
            response.timeZone?.dstEnd,
            success,
            error,
            apiResponse,
            requestData,
            responseData,
            response,
            lookupRequest,
            bulkRequest,
        )

        covered.filterNotNull().forEach { value ->
            invokeZeroArgMethods(value)
            assertThat(value.toString()).isNotBlank()
            assertThat(value).isEqualTo(value)
            assertThat(value.hashCode()).isEqualTo(value.hashCode())
        }

        assertThat(device.copy(name = "iPhone")).isNotEqualTo(device)
        assertThat(engine.copy(version = "618.0")).isNotEqualTo(engine)
        assertThat(operatingSystem.copy(build = "24A100")).isNotEqualTo(operatingSystem)
        assertThat(userAgent.copy(version = "18.0")).isNotEqualTo(userAgent)
        assertThat(location.copy(city = "Sunnyvale")).isNotEqualTo(location)
        assertThat(response.copy(ip = "1.1.1.1")).isNotEqualTo(response)
        assertThat(success.isSuccess()).isTrue()
        assertThat(error.isSuccess()).isFalse()
        assertThat(BulkLookupResult(data = response, error = BulkLookupError("ambiguous")).isSuccess()).isFalse()
        assertThat(apiResponse.copy(data = response.copy(ip = "9.9.9.9"))).isNotEqualTo(apiResponse)
        assertThat(requestData.copy(method = "POST")).isNotEqualTo(requestData)
        assertThat(responseData.copy(statusCode = 201)).isNotEqualTo(responseData)
        assertThat(lookupRequest.copy(ip = "1.1.1.1")).isNotEqualTo(lookupRequest)
        assertThat(bulkRequest.copy(ips = listOf("9.9.9.9"))).isNotEqualTo(bulkRequest)
    }

    private fun invokeZeroArgMethods(value: Any) {
        value.javaClass.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
            .filter { it.parameterCount == 0 }
            .filterNot { it.name in setOf("wait", "notify", "notifyAll", "getClass") }
            .forEach { method ->
                method.invoke(value)
            }
    }
}
