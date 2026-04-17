package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.ValidationException
import io.ipgeolocation.sdk.internal.ObjectMappers
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class RequestValidationTest {
    @Test
    fun typedLookupRejectsXmlOutputBeforeIo() {
        val executor = TestHttpExecutor()
        val client = clientWith(executor)

        assertThatThrownBy {
            client.lookupIpGeolocation(LookupIpGeolocationRequest(output = ResponseFormat.XML))
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("XML output is not supported")

        assertThat(executor.capturedRequests).isEmpty()
    }

    @Test
    fun typedBulkRejectsXmlOutputBeforeIo() {
        val executor = TestHttpExecutor()
        val client = clientWith(executor)

        assertThatThrownBy {
            client.bulkLookupIpGeolocation(
                BulkLookupIpGeolocationRequest(
                    ips = listOf("8.8.8.8"),
                    output = ResponseFormat.XML,
                ),
            )
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("XML output is not supported")

        assertThat(executor.capturedRequests).isEmpty()
    }

    @Test
    fun rawMethodsAllowXmlOutput() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, "<ipgeo><ip>8.8.8.8</ip></ipgeo>")
            enqueueResponse(200, "<items><item><ip>8.8.8.8</ip></item></items>")
        }
        val client = clientWith(executor)

        val single = client.lookupIpGeolocationRaw(
            LookupIpGeolocationRequest(ip = "8.8.8.8", output = ResponseFormat.XML),
        )
        val bulk = client.bulkLookupIpGeolocationRaw(
            BulkLookupIpGeolocationRequest(ips = listOf("8.8.8.8"), output = ResponseFormat.XML),
        )

        assertThat(single.data).contains("<ipgeo>")
        assertThat(bulk.data).contains("<items>")
        assertThat(executor.capturedRequests).hasSize(2)
        assertThat(executor.capturedRequests[0].headers["Accept"]).containsExactly("application/xml")
        assertThat(executor.capturedRequests[1].headers["Accept"]).containsExactly("application/xml")
    }

    @Test
    fun singleLookupRequiresApiKeyOrRequestOrigin() {
        val executor = TestHttpExecutor()
        val client = clientWith(executor, IpGeolocationClientConfig())

        assertThatThrownBy { client.lookupIpGeolocation() }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("single lookup requires apiKey or requestOrigin")

        assertThat(executor.capturedRequests).isEmpty()
    }

    @Test
    fun bulkLookupRequiresApiKey() {
        val executor = TestHttpExecutor()
        val client = clientWith(executor, IpGeolocationClientConfig(requestOrigin = "https://app.example.com"))

        assertThatThrownBy {
            client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(ips = listOf("8.8.8.8")))
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("bulk lookup requires apiKey")

        assertThat(executor.capturedRequests).isEmpty()
    }

    @Test
    fun closedClientRejectsFurtherCallsAndClosesExecutor() {
        val executor = TestHttpExecutor()
        val client = clientWith(executor)

        client.close()
        client.close()

        assertThat(executor.closed).isTrue()
        assertThatThrownBy { client.lookupIpGeolocation() }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("client is closed")
    }

    private fun clientWith(
        executor: TestHttpExecutor,
        config: IpGeolocationClientConfig = IpGeolocationClientConfig(apiKey = "test-key"),
    ): IpGeolocationClient {
        return IpGeolocationClient(config, executor, ObjectMappers.default())
    }
}
