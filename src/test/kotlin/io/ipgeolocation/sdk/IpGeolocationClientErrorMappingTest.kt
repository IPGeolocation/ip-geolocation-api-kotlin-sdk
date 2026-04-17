package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.BadRequestException
import io.ipgeolocation.sdk.exceptions.UnauthorizedException
import io.ipgeolocation.sdk.internal.ObjectMappers
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class IpGeolocationClientErrorMappingTest {
    @Test
    fun maps401ToUnauthorizedAndPreservesNestedApiMessage() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(
                401,
                """
                    {
                      "error": {
                        "message": "invalid key"
                      }
                    }
                """.trimIndent(),
            )
        }
        val client = clientWith(executor)

        assertThatThrownBy { client.lookupIpGeolocation() }
            .isInstanceOfSatisfying(UnauthorizedException::class.java) { error ->
                assertThat(error.statusCode).isEqualTo(401)
                assertThat(error.apiMessage).isEqualTo("invalid key")
                assertThat(error.message).contains("HTTP status 401")
            }
    }

    @Test
    fun nonJsonErrorBodyIsTruncatedForApiMessage() {
        val longBody = buildString {
            repeat(700) { append('x') }
        }
        val executor = TestHttpExecutor().apply {
            enqueueResponse(400, longBody)
        }
        val client = clientWith(executor)

        assertThatThrownBy { client.lookupIpGeolocation() }
            .isInstanceOfSatisfying(BadRequestException::class.java) { error ->
                assertThat(error.apiMessage).hasSize(512)
                assertThat(error.message).contains("HTTP status 400")
            }
    }

    private fun clientWith(executor: TestHttpExecutor): IpGeolocationClient {
        return IpGeolocationClient(
            IpGeolocationClientConfig(apiKey = "test-key"),
            executor,
            ObjectMappers.default(),
        )
    }
}
