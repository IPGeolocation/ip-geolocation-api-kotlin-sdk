package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.BadRequestException
import io.ipgeolocation.sdk.exceptions.ValidationException
import io.ipgeolocation.sdk.internal.ObjectMappers
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Properties

class IpGeolocationClientCoverageTest {
    @Test
    fun rawAndBulkMethodsMapNon2xxResponses() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(400, """{"message":"invalid request"}""")
            enqueueResponse(400, """{"message":"bulk failed"}""")
            enqueueResponse(400, """{"message":"bulk raw failed"}""")
        }
        val client = clientWith(executor)

        assertThatThrownBy { client.lookupIpGeolocationRaw(LookupIpGeolocationRequest()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("invalid request")

        assertThatThrownBy {
            client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(ips = listOf("8.8.8.8")))
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("bulk failed")

        assertThatThrownBy {
            client.bulkLookupIpGeolocationRaw(BulkLookupIpGeolocationRequest(ips = listOf("8.8.8.8")))
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("bulk raw failed")
    }

    @Test
    fun nullableRequestOverloadsRejectNullBeforeIo() {
        val executor = TestHttpExecutor()
        val client = clientWith(executor)

        assertThatThrownBy { client.lookupIpGeolocation(null) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("request must not be null")

        assertThatThrownBy { client.lookupIpGeolocationRaw(null) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("request must not be null")

        assertThatThrownBy { client.bulkLookupIpGeolocation(null) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("request must not be null")

        assertThatThrownBy { client.bulkLookupIpGeolocationRaw(null) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("request must not be null")

        assertThat(executor.capturedRequests).isEmpty()
    }

    @Test
    fun requestOriginAndUserAgentRulesStayAuthoritative() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, """{"ip":"8.8.8.8"}""")
            enqueueResponse(200, """{"ip":"8.8.8.8"}""")
        }
        val client = clientWith(
            executor = executor,
            config = IpGeolocationClientConfig(requestOrigin = "https://good.example.com"),
        )

        client.lookupIpGeolocation(
            LookupIpGeolocationRequest(
                ip = "8.8.8.8",
                headers = mapOf(
                    "Origin" to "https://evil.example.com",
                    "User-Agent" to "HeaderAgent/1.0",
                ),
            ),
        )

        client.lookupIpGeolocation(
            LookupIpGeolocationRequest(
                ip = "8.8.8.8",
                userAgent = "FieldAgent/2.0",
                headers = mapOf("User-Agent" to "HeaderAgent/1.0"),
            ),
        )

        assertThat(executor.capturedRequests[0].headers["Origin"]).containsExactly("https://good.example.com")
        assertThat(executor.capturedRequests[0].headers["User-Agent"]).containsExactly("HeaderAgent/1.0")
        assertThat(executor.capturedRequests[1].headers["User-Agent"]).containsExactly("FieldAgent/2.0")
    }

    @Test
    fun defaultUserAgentIsStableAndSingleLookupCanUseRequestOriginOnly() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, """{"ip":"8.8.8.8"}""")
        }
        val client = clientWith(
            executor = executor,
            config = IpGeolocationClientConfig(requestOrigin = "https://app.example.com"),
        )

        val response = client.lookupIpGeolocation()
        val expectedVersion = Properties().apply {
            IpGeolocationClient::class.java.getResourceAsStream("/io/ipgeolocation/sdk/version.properties").use { input ->
                requireNotNull(input) { "version.properties must be present on the classpath" }
                load(input)
            }
        }.getProperty("sdk.version")

        assertThat(IpGeolocationClient.defaultUserAgent()).isEqualTo("ipgeolocation-kotlin-sdk/$expectedVersion")
        assertThat(response.data.ip).isEqualTo("8.8.8.8")
        assertThat(executor.capturedRequests.single().url.toString()).doesNotContain("apiKey=")
        assertThat(executor.capturedRequests.single().headers["Origin"]).containsExactly("https://app.example.com")
    }

    @Test
    fun emptyFiltersAreOmittedFromSingleAndBulkUrls() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, """{"ip":"8.8.8.8"}""")
            enqueueResponse(200, """[{"ip":"8.8.8.8"}]""", headers("X-Successful-Record", "1"))
        }
        val client = clientWith(executor, IpGeolocationClientConfig(apiKey = "test-key"))

        client.lookupIpGeolocation(
            LookupIpGeolocationRequest(
                ip = "8.8.8.8",
                include = emptyList(),
                fields = emptyList(),
                excludes = emptyList(),
            ),
        )
        client.bulkLookupIpGeolocation(
            BulkLookupIpGeolocationRequest(
                ips = listOf("8.8.8.8"),
                include = emptyList(),
                fields = emptyList(),
                excludes = emptyList(),
            ),
        )

        assertThat(executor.capturedRequests[0].url.toString()).doesNotContain("include=", "fields=", "excludes=")
        assertThat(executor.capturedRequests[1].url.toString()).doesNotContain("include=", "fields=", "excludes=")
        assertThat(executor.capturedRequests[1].headers).doesNotContainKey("Origin")
    }

    private fun clientWith(
        executor: TestHttpExecutor,
        config: IpGeolocationClientConfig = IpGeolocationClientConfig(apiKey = "test-key"),
    ): IpGeolocationClient {
        return IpGeolocationClient(config, executor, ObjectMappers.default())
    }
}
