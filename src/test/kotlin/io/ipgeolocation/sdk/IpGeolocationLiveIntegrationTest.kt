package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.UnauthorizedException
import io.ipgeolocation.sdk.exceptions.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class IpGeolocationLiveIntegrationTest {
    companion object {
        private lateinit var freeKey: String
        private lateinit var paidKey: String

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            assumeTrue(
                "true".equals(System.getenv("IPGEO_RUN_LIVE_TESTS"), ignoreCase = true),
                "Set IPGEO_RUN_LIVE_TESTS=true to enable live tests",
            )

            freeKey = System.getenv("IPGEO_FREE_KEY").orEmpty()
            paidKey = System.getenv("IPGEO_PAID_KEY").orEmpty()

            assumeTrue(!freeKey.isBlank(), "IPGEO_FREE_KEY is required")
            assumeTrue(!paidKey.isBlank(), "IPGEO_PAID_KEY is required")
        }
    }

    @Test
    fun freePlanBaseLookupWorks() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = freeKey)).use { client ->
            val response = client.lookupIpGeolocation(LookupIpGeolocationRequest(ip = "8.8.8.8"))

            assertThat(response.data.ip).isEqualTo("8.8.8.8")
            assertThat(response.metadata.creditsCharged).isGreaterThanOrEqualTo(1)
            assertThat(response.data.timeZone).isNotNull
            assertThat(response.data.timeZone?.currentTzAbbreviation).isNotBlank()
            assertThat(response.data.timeZone?.currentTzFullName).isNotBlank()
        }
    }

    @Test
    fun freePlanSecurityModuleIsRejected() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = freeKey)).use { client ->
            assertFreePlanIncludeRejected(client, "security")
        }
    }

    @Test
    fun freePlanIncludeStarReturnsDefaultResponseWithoutAdditionalModules() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = freeKey)).use { client ->
            val response = client.lookupIpGeolocation(
                LookupIpGeolocationRequest(
                    ip = "8.8.8.8",
                    include = listOf("*"),
                ),
            )

            assertThat(response.data.ip).isEqualTo("8.8.8.8")
            assertThat(response.data.security).isNull()
            assertThat(response.data.abuse).isNull()
            assertThat(response.data.userAgent).isNull()
        }
    }

    @Test
    fun freePlanBulkIsRejected() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = freeKey)).use { client ->
            assertThatThrownBy {
                client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(ips = listOf("8.8.8.8")))
            }
                .isInstanceOf(UnauthorizedException::class.java)
        }
    }

    @Test
    fun paidPlanSecurityAndAbuseWorks() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = paidKey)).use { client ->
            val response = client.lookupIpGeolocation(
                LookupIpGeolocationRequest(
                    ip = "8.8.8.8",
                    include = listOf("security", "abuse"),
                ),
            )

            assertThat(response.data.security).isNotNull
            assertThat(response.data.abuse).isNotNull
            assertThat(response.metadata.creditsCharged).isGreaterThanOrEqualTo(1)
        }
    }

    @Test
    fun paidPlanBulkMixedReturnsSuccessAndErrorItems() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = paidKey)).use { client ->
            val response = client.bulkLookupIpGeolocation(
                BulkLookupIpGeolocationRequest(
                    ips = listOf("8.8.8.8", "invalid-ip"),
                ),
            )

            assertThat(response.data).hasSize(2)
            assertThat(response.data[0].isSuccess()).isTrue()
            assertThat(response.data[0].data?.ip).isEqualTo("8.8.8.8")
            assertThat(response.data[1].error?.message).isNotBlank()
        }
    }

    @Test
    fun paidPlanXmlOutputIsRejectedForTypedMethod() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = paidKey)).use { client ->
            assertThatThrownBy {
                client.lookupIpGeolocation(
                    LookupIpGeolocationRequest(
                        ip = "8.8.8.8",
                        output = ResponseFormat.XML,
                    ),
                )
            }
                .isInstanceOf(ValidationException::class.java)
                .hasMessageContaining("XML output is not supported")
        }
    }

    @Test
    fun paidPlanIncludeUserAgentReflectsRequestHeaderOverride() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = paidKey)).use { client ->
            val defaultUaResponse = client.lookupIpGeolocation(
                LookupIpGeolocationRequest(
                    ip = "8.8.8.8",
                    include = listOf("user_agent"),
                ),
            )

            val overrideUserAgent = "python-requests/2.32.5"
            val overriddenUaResponse = client.lookupIpGeolocation(
                LookupIpGeolocationRequest(
                    ip = "8.8.8.8",
                    include = listOf("user_agent"),
                    userAgent = overrideUserAgent,
                ),
            )

            assertThat(defaultUaResponse.data.userAgent).isNotNull
            assertThat(defaultUaResponse.data.userAgent?.userAgentString).isNotBlank()
            assertThat(overriddenUaResponse.data.userAgent).isNotNull
            assertThat(overriddenUaResponse.data.userAgent?.userAgentString).isEqualTo(overrideUserAgent)
            assertThat(overriddenUaResponse.data.userAgent?.userAgentString)
                .isNotEqualTo(defaultUaResponse.data.userAgent?.userAgentString)
        }
    }

    @Test
    fun paidPlanRawMethodsReturnXmlWhenRequested() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = paidKey)).use { client ->
            val single = client.lookupIpGeolocationRaw(
                LookupIpGeolocationRequest(
                    ip = "8.8.8.8",
                    output = ResponseFormat.XML,
                ),
            )
            assertThat(single.data).contains("<")

            val bulk = client.bulkLookupIpGeolocationRaw(
                BulkLookupIpGeolocationRequest(
                    ips = listOf("8.8.8.8", "invalid-ip"),
                    output = ResponseFormat.XML,
                ),
            )
            assertThat(bulk.data).contains("<")
        }
    }

    private fun assertFreePlanIncludeRejected(client: IpGeolocationClient, includeValue: String) {
        assertThatThrownBy {
            client.lookupIpGeolocation(
                LookupIpGeolocationRequest(
                    ip = "8.8.8.8",
                    include = listOf(includeValue),
                ),
            )
        }
            .isInstanceOfSatisfying(UnauthorizedException::class.java) { error ->
                assertThat(error.statusCode).isEqualTo(401)
            }
    }
}
