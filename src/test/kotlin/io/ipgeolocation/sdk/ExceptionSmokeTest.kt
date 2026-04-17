package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.ApiException
import io.ipgeolocation.sdk.exceptions.ClientClosedRequestException
import io.ipgeolocation.sdk.exceptions.IpGeolocationException
import io.ipgeolocation.sdk.exceptions.LockedException
import io.ipgeolocation.sdk.exceptions.MethodNotAllowedException
import io.ipgeolocation.sdk.exceptions.NotFoundException
import io.ipgeolocation.sdk.exceptions.PayloadTooLargeException
import io.ipgeolocation.sdk.exceptions.RateLimitException
import io.ipgeolocation.sdk.exceptions.RequestTimeoutException
import io.ipgeolocation.sdk.exceptions.SerializationException
import io.ipgeolocation.sdk.exceptions.ServerErrorException
import io.ipgeolocation.sdk.exceptions.TransportException
import io.ipgeolocation.sdk.exceptions.UnsupportedMediaTypeException
import io.ipgeolocation.sdk.exceptions.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExceptionSmokeTest {
    @Test
    fun typedApiExceptionsKeepMessageAndStatusDetails() {
        val exceptions = listOf<ApiException>(
            NotFoundException("not found", 404, "not found"),
            MethodNotAllowedException("method", 405, "method"),
            PayloadTooLargeException("too large", 413, "too large"),
            UnsupportedMediaTypeException("unsupported", 415, "unsupported"),
            LockedException("locked", 423, "locked"),
            RateLimitException("rate limit", 429, "rate limit"),
            ClientClosedRequestException("closed", 499, "closed"),
            ServerErrorException("server", 500, "server"),
        )

        exceptions.forEach { exception ->
            assertThat(exception.message).isNotBlank()
            assertThat(exception.statusCode).isGreaterThanOrEqualTo(400)
            assertThat(exception.apiMessage).isNotBlank()
        }
    }

    @Test
    fun baseTransportAndValidationExceptionsPreserveCauseAndMessage() {
        val cause = IllegalStateException("boom")

        val base = IpGeolocationException("base")
        val withCause = IpGeolocationException("base-with-cause", cause)
        val transport = TransportException("transport", cause)
        val timeout = RequestTimeoutException("timeout", cause)
        val serialization = SerializationException("serialize", cause)
        val validation = ValidationException("validation")

        assertThat(base.message).isEqualTo("base")
        assertThat(withCause.cause).isSameAs(cause)
        assertThat(transport.cause).isSameAs(cause)
        assertThat(timeout.cause).isSameAs(cause)
        assertThat(serialization.cause).isSameAs(cause)
        assertThat(validation.message).isEqualTo("validation")
    }
}
