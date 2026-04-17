package io.ipgeolocation.sdk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ipgeolocation.sdk.internal.ObjectMappers
import io.ipgeolocation.sdk.model.BulkLookupResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class IpGeolocationLiveFieldParityTest {
    companion object {
        private val mapper: ObjectMapper = ObjectMappers.default()
        private lateinit var paidKey: String

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            assumeTrue(
                "true".equals(System.getenv("IPGEO_RUN_LIVE_HARDENING"), ignoreCase = true),
                "Set IPGEO_RUN_LIVE_HARDENING=true to enable live field parity tests",
            )

            paidKey = System.getenv("IPGEO_PAID_KEY").orEmpty()
            assumeTrue(!paidKey.isBlank(), "IPGEO_PAID_KEY is required")
        }
    }

    @Test
    fun includeStarResponseMatchesTypedModel() {
        assertSingleLookupParity(
            LookupIpGeolocationRequest(
                ip = "8.8.8.8",
                include = listOf("*"),
            ),
        )
    }

    @Test
    fun geoAccuracyAndDmaResponseMatchesTypedModel() {
        assertSingleLookupParity(
            LookupIpGeolocationRequest(
                ip = "8.8.8.8",
                include = listOf("geo_accuracy", "dma_code"),
            ),
        )
    }

    @Test
    fun domainLookupResponseMatchesTypedModel() {
        assertSingleLookupParity(
            LookupIpGeolocationRequest(
                ip = "ipgeolocation.io",
                include = listOf("hostnameFallbackLive"),
            ),
        )
    }

    @Test
    fun securityAbuseAndUserAgentResponseMatchesTypedModel() {
        assertSingleLookupParity(
            LookupIpGeolocationRequest(
                ip = "8.8.8.8",
                include = listOf("security", "abuse", "user_agent"),
                userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/601.3.9 (KHTML, like Gecko) Version/9.0.2 Safari/601.3.9",
            ),
        )
    }

    @Test
    fun bulkMixedResponseMatchesTypedModel() {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = paidKey)).use { client ->
            val request = BulkLookupIpGeolocationRequest(
                ips = listOf("8.8.8.8", "invalid-ip", "1.1.1.1"),
            )

            val raw = client.bulkLookupIpGeolocationRaw(request)
            val typed = client.bulkLookupIpGeolocation(request)

            val rawArray = mapper.readTree(raw.data)
            assertThat(rawArray.isArray).isTrue()
            assertThat(typed.data).hasSize(rawArray.size())

            rawArray.forEachIndexed { index, rawItem ->
                val typedItem: BulkLookupResult = typed.data[index]
                if (typedItem.isSuccess()) {
                    assertJsonSubset(rawItem, mapper.valueToTree(typedItem.data), "$[$index]")
                } else {
                    val rawMessage = rawItem.path("error").path("message").takeUnless { it.isMissingNode || it.isNull }?.asText()
                        ?: rawItem.path("message").takeUnless { it.isMissingNode || it.isNull }?.asText()
                    assertThat(rawMessage).isEqualTo(typedItem.error?.message)
                }
            }
        }
    }

    private fun assertSingleLookupParity(request: LookupIpGeolocationRequest) {
        IpGeolocationClient(IpGeolocationClientConfig(apiKey = paidKey)).use { client ->
            val raw = client.lookupIpGeolocationRaw(request)
            val typed = client.lookupIpGeolocation(request)

            val rawNode = mapper.readTree(raw.data)
            val typedNode = mapper.valueToTree<JsonNode>(typed.data)

            assertJsonSubset(rawNode, typedNode, "$")
        }
    }

    private fun assertJsonSubset(rawNode: JsonNode?, typedNode: JsonNode?, path: String) {
        if (rawNode == null || rawNode.isNull) {
            assertThat(typedNode == null || typedNode.isNull).describedAs(path).isTrue()
            return
        }

        assertThat(typedNode).describedAs(path).isNotNull

        if (isLiveClockField(path)) {
            assertThat(typedNode!!.isNull).describedAs(path).isFalse()
            return
        }

        if (rawNode.isObject) {
            assertThat(typedNode!!.isObject).describedAs(path).isTrue()
            rawNode.properties().forEach { entry ->
                assertThat(typedNode.has(entry.key)).describedAs("$path.${entry.key}").isTrue()
                assertJsonSubset(entry.value, typedNode.get(entry.key), "$path.${entry.key}")
            }
            return
        }

        if (rawNode.isArray) {
            assertThat(typedNode!!.isArray).describedAs(path).isTrue()
            assertThat(typedNode.size()).describedAs("$path size").isEqualTo(rawNode.size())
            rawNode.forEachIndexed { index, child ->
                assertJsonSubset(child, typedNode.get(index), "$path[$index]")
            }
            return
        }

        if (rawNode.isNumber && typedNode!!.isNumber) {
            val rawValue = rawNode.decimalValue().stripTrailingZeros()
            val typedValue = typedNode.decimalValue().stripTrailingZeros()
            assertThat(typedValue).describedAs(path).isEqualByComparingTo(rawValue)
            return
        }

        assertThat(typedNode).describedAs(path).isEqualTo(rawNode)
    }

    private fun isLiveClockField(path: String): Boolean {
        return path.endsWith(".time_zone.current_time") || path.endsWith(".time_zone.current_time_unix")
    }
}
