package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.BadRequestException
import io.ipgeolocation.sdk.exceptions.RequestTimeoutException
import io.ipgeolocation.sdk.exceptions.TransportException
import io.ipgeolocation.sdk.internal.ObjectMappers
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.SocketTimeoutException

class IpGeolocationClientTransportTest {
    @Test
    fun timeoutIOExceptionMapsToRequestTimeoutException() {
        val executor = TestHttpExecutor().apply {
            enqueueThrowable(SocketTimeoutException("socket timed out"))
        }
        val client = clientWith(executor)

        assertThatThrownBy { client.lookupIpGeolocation() }
            .isInstanceOf(RequestTimeoutException::class.java)
            .hasMessageContaining("timed out")
    }

    @Test
    fun genericIOExceptionMapsToTransportException() {
        val executor = TestHttpExecutor().apply {
            enqueueThrowable(IOException("connection reset"))
        }
        val client = clientWith(executor)

        assertThatThrownBy { client.lookupIpGeolocation() }
            .isInstanceOf(TransportException::class.java)
            .hasMessageContaining("HTTP transport error")
    }

    @Test
    fun apiAndRuntimeExceptionsPassThroughUnwrapped() {
        val apiExecutor = TestHttpExecutor().apply {
            enqueueThrowable(BadRequestException("bad request", 400, "bad request"))
        }
        val runtimeExecutor = TestHttpExecutor().apply {
            enqueueThrowable(IllegalStateException("boom"))
        }

        assertThatThrownBy { clientWith(apiExecutor).lookupIpGeolocation() }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("bad request")

        assertThatThrownBy { clientWith(runtimeExecutor).lookupIpGeolocation() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("boom")
    }

    private fun clientWith(executor: TestHttpExecutor): IpGeolocationClient {
        return IpGeolocationClient(
            IpGeolocationClientConfig(apiKey = "test-key"),
            executor,
            ObjectMappers.default(),
        )
    }
}
