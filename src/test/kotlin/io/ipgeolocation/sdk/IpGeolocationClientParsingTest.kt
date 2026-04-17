package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.SerializationException
import io.ipgeolocation.sdk.internal.ObjectMappers
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class IpGeolocationClientParsingTest {
    @Test
    fun parsesSingleLookupResponseAndMetadata() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(
                statusCode = 200,
                body = """
                    {
                      "ip": "91.128.103.196",
                      "location": {
                        "country_name": "Sweden",
                        "is_eu": true,
                        "confidence": "high"
                      },
                      "country_metadata": {
                        "calling_code": "+46",
                        "tld": ".se",
                        "languages": ["sv-SE"]
                      },
                      "security": {
                        "is_vpn": true,
                        "vpn_provider_names": ["ExampleVPN"],
                        "threat_score": 12.5
                      },
                      "time_zone": {
                        "name": "Europe/Stockholm",
                        "is_dst": false,
                        "dst_start": {}
                      }
                    }
                """.trimIndent(),
                headers = headers(
                    "X-Credits-Charged", "1",
                    "X-Successful-Record", "1",
                    "X-Trace-Id", "trace-1",
                ),
            )
        }
        val client = clientWith(executor)

        val response = client.lookupIpGeolocation()

        assertThat(response.data.ip).isEqualTo("91.128.103.196")
        assertThat(response.data.location?.countryName).isEqualTo("Sweden")
        assertThat(response.data.location?.isEu).isTrue()
        assertThat(response.data.location?.confidence).isEqualTo("high")
        assertThat(response.data.countryMetadata?.languages).containsExactly("sv-SE")
        assertThat(response.data.security?.isVpn).isTrue()
        assertThat(response.data.security?.vpnProviderNames).containsExactly("ExampleVPN")
        assertThat(response.data.security?.proxyProviderNames).isNull()
        assertThat(response.data.timeZone?.name).isEqualTo("Europe/Stockholm")
        assertThat(response.data.timeZone?.isDst).isFalse()
        assertThat(response.data.timeZone?.dstStart).isNotNull()
        assertThat(response.metadata.creditsCharged).isEqualTo(1)
        assertThat(response.metadata.successfulRecords).isEqualTo(1)
        assertThat(response.metadata.statusCode).isEqualTo(200)
        assertThat(response.metadata.durationMs).isGreaterThanOrEqualTo(0L)
        assertThat(response.metadata.firstHeaderValue("x-trace-id")).isEqualTo("trace-1")
    }

    @Test
    fun parsesMixedBulkResultsUsingDataAndNestedErrorMessage() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(
                statusCode = 200,
                body = """
                    [
                      {
                        "ip": "8.8.8.8",
                        "location": {
                          "country_name": "United States"
                        }
                      },
                      {
                        "error": {
                          "message": "Invalid IP address"
                        }
                      }
                    ]
                """.trimIndent(),
                headers = headers("X-Successful-Records", "1"),
            )
        }
        val client = clientWith(executor)

        val response = client.bulkLookupIpGeolocation(
            BulkLookupIpGeolocationRequest(ips = listOf("8.8.8.8", "bad-ip")),
        )

        assertThat(response.data).hasSize(2)
        assertThat(response.data[0].isSuccess()).isTrue()
        assertThat(response.data[0].data?.ip).isEqualTo("8.8.8.8")
        assertThat(response.data[0].error).isNull()
        assertThat(response.data[1].isSuccess()).isFalse()
        assertThat(response.data[1].data).isNull()
        assertThat(response.data[1].error?.message).isEqualTo("Invalid IP address")
        assertThat(response.metadata.successfulRecords).isEqualTo(1)
    }

    @Test
    fun typedBulkResponseRejectsNonArrayPayload() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, """{"ip":"8.8.8.8"}""")
        }
        val client = clientWith(executor)

        assertThatThrownBy {
            client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(ips = listOf("8.8.8.8")))
        }
            .isInstanceOf(SerializationException::class.java)
            .hasMessageContaining("expected an array payload")
    }

    @Test
    fun singleTypedResponseRejectsMalformedJson() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, "{malformed-json")
        }
        val client = clientWith(executor)

        assertThatThrownBy { client.lookupIpGeolocation() }
            .isInstanceOf(SerializationException::class.java)
            .hasMessageContaining("Failed to deserialize API response")
    }

    @Test
    fun requestUsesConfiguredReadTimeout() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, """{"ip":"8.8.8.8"}""")
        }
        val config = IpGeolocationClientConfig(
            apiKey = "test-key",
            connectTimeout = Duration.ofSeconds(5),
            readTimeout = Duration.ofSeconds(7),
        )
        val client = clientWith(executor, config)

        client.lookupIpGeolocation()

        assertThat(executor.capturedTimeouts).containsExactly(Duration.ofSeconds(7))
    }

    private fun clientWith(
        executor: TestHttpExecutor,
        config: IpGeolocationClientConfig = IpGeolocationClientConfig(apiKey = "test-key"),
    ): IpGeolocationClient {
        return IpGeolocationClient(config, executor, ObjectMappers.default())
    }
}
