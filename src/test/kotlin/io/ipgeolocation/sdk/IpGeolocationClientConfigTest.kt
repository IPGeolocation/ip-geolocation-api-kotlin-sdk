package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class IpGeolocationClientConfigTest {
    @Test
    fun defaultsAndSafeMapRedactSecrets() {
        val config = IpGeolocationClientConfig(apiKey = "  test-key  ")

        assertThat(config.apiKey).isEqualTo("test-key")
        assertThat(config.requestOrigin).isNull()
        assertThat(config.baseUrl).isEqualTo(IpGeolocationClientConfig.DEFAULT_BASE_URL)
        assertThat(config.connectTimeout).isEqualTo(IpGeolocationClientConfig.DEFAULT_CONNECT_TIMEOUT)
        assertThat(config.readTimeout).isEqualTo(IpGeolocationClientConfig.DEFAULT_READ_TIMEOUT)
        assertThat(config.maxResponseBodyChars).isEqualTo(IpGeolocationClientConfig.DEFAULT_MAX_RESPONSE_BODY_CHARS)
        assertThat(config.safeMap()).containsEntry("apiKey", "[REDACTED]")
        assertThat(config.safeMap()).containsEntry("readTimeoutMs", 30_000L)
        assertThat(config.safeMap()).containsEntry("maxResponseBodyChars", IpGeolocationClientConfig.DEFAULT_MAX_RESPONSE_BODY_CHARS)
        assertThat(config.toString()).doesNotContain("test-key").contains("[REDACTED]")
    }

    @Test
    fun normalizesOriginsBaseUrlAndTimeouts() {
        val config = IpGeolocationClientConfig(
            apiKey = "key",
            requestOrigin = " https://app.example.com:8443/ ",
            baseUrl = " https://api.example.com/ ",
            connectTimeout = Duration.ofSeconds(2),
            readTimeout = Duration.ofSeconds(5),
            maxResponseBodyChars = 2048,
        )

        assertThat(config.requestOrigin).isEqualTo("https://app.example.com:8443")
        assertThat(config.baseUrl).isEqualTo("https://api.example.com")
        assertThat(config.connectTimeout).isEqualTo(Duration.ofSeconds(2))
        assertThat(config.readTimeout).isEqualTo(Duration.ofSeconds(5))
        assertThat(config.maxResponseBodyChars).isEqualTo(2048)
    }

    @Test
    fun rejectsInvalidConfigValues() {
        assertThatThrownBy { IpGeolocationClientConfig(apiKey = " ") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("apiKey")

        assertThatThrownBy { IpGeolocationClientConfig(requestOrigin = " ") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("requestOrigin")

        assertThatThrownBy { IpGeolocationClientConfig(requestOrigin = "https://app.example.com/path") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("path")

        assertThatThrownBy { IpGeolocationClientConfig(requestOrigin = "ftp://app.example.com") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("absolute http or https origin")

        assertThatThrownBy { IpGeolocationClientConfig(requestOrigin = "https://user@app.example.com") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("userinfo")

        assertThatThrownBy { IpGeolocationClientConfig(requestOrigin = "https://app.example.com?x=1") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("query or fragment")

        assertThatThrownBy { IpGeolocationClientConfig(requestOrigin = "https://app.example.com#frag") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("query or fragment")

        assertThatThrownBy { IpGeolocationClientConfig(requestOrigin = "http:///only-path") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("valid host")

        assertThatThrownBy { IpGeolocationClientConfig(requestOrigin = "https://app.example.com\r\nx") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("CR or LF")

        assertThatThrownBy { IpGeolocationClientConfig(baseUrl = " ") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("baseUrl")

        assertThatThrownBy { IpGeolocationClientConfig(baseUrl = "https://user@api.example.com") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("userinfo")

        assertThatThrownBy { IpGeolocationClientConfig(baseUrl = "https://api.example.com?x=1") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("query or fragment")

        assertThatThrownBy { IpGeolocationClientConfig(baseUrl = "https://api.example.com#frag") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("query or fragment")

        assertThatThrownBy { IpGeolocationClientConfig(baseUrl = "http:///missing-host") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("valid host")

        assertThatThrownBy { IpGeolocationClientConfig(connectTimeout = Duration.ZERO) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("connectTimeout")

        assertThatThrownBy { IpGeolocationClientConfig(readTimeout = Duration.ofMillis(-1)) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("readTimeout")

        assertThatThrownBy { IpGeolocationClientConfig(maxResponseBodyChars = 0) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("maxResponseBodyChars")

        assertThatThrownBy {
            IpGeolocationClientConfig(
                connectTimeout = Duration.ofSeconds(6),
                readTimeout = Duration.ofSeconds(5),
            )
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("connectTimeout must be <= readTimeout")

        assertThat(IpGeolocationClientConfig().safeMap()).containsEntry("apiKey", null)
    }
}
