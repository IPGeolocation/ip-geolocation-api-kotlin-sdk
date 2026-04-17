package io.ipgeolocation.sdk.internal

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CompatTest {
    @Test
    fun normalizationHelpersHandleWhitespaceAndDeduplication() {
        assertThat(containsCrOrLf("value\r\nnext")).isTrue()
        assertThat(containsCrOrLf("value")).isFalse()
        assertThat(normalizeOptionalHeaderValue(" Agent/1.0 ", "userAgent")).isEqualTo("Agent/1.0")
        assertThat(normalizeTokenList(listOf("security", " security ", "abuse"), "include"))
            .containsExactly("security", "abuse")
    }

    @Test
    fun normalizationHelpersRejectBlankAndCrlfValues() {
        assertThatThrownBy { normalizeOptionalHeaderValue(" ", "userAgent") }
            .hasMessageContaining("userAgent")

        assertThatThrownBy { normalizeOptionalHeaderValue("bad\r\nvalue", "userAgent") }
            .hasMessageContaining("CR or LF")

        assertThatThrownBy { normalizeTokenList(listOf(" "), "include") }
            .hasMessageContaining("include")

        assertThatThrownBy { normalizeHeaders(mapOf(" " to "value")) }
            .hasMessageContaining("blank names")

        assertThatThrownBy { normalizeHeaders(mapOf("X-Test" to "bad\r\nvalue")) }
            .hasMessageContaining("CR or LF")
    }

    @Test
    fun mergeHeadersResolveUserAgentAndDefaultCloseWorkAsExpected() {
        val normalized = normalizeHeaders(linkedMapOf(" X-Test " to " value ", "User-Agent" to "HeaderAgent/1.0"))
        assertThat(normalized).containsEntry("X-Test", "value")
        assertThat(normalizeHeaders(emptyMap())).isEmpty()

        val merged = mergeHeaders(
            mapOf("Origin" to listOf("https://good.example.com")),
            mapOf("" to listOf("ignored")),
            mapOf("origin" to listOf("https://override.example.com")),
            mapOf("User-Agent" to listOf("HeaderAgent/1.0")),
            null,
        )

        assertThat(merged).doesNotContainKey("Origin")
        assertThat(merged).containsEntry("origin", listOf("https://override.example.com"))
        assertThat(resolveUserAgentHeader("FieldAgent/2.0", normalized, "Default/1.0")).isEqualTo("FieldAgent/2.0")
        assertThat(resolveUserAgentHeader(null, normalized, "Default/1.0")).isEqualTo("HeaderAgent/1.0")
        assertThat(resolveUserAgentHeader(null, emptyMap(), "Default/1.0")).isEqualTo("Default/1.0")

        val executor = object : HttpExecutor {
            override fun send(request: HttpRequestData): HttpResponseData {
                error("not needed")
            }
        }
        executor.close()
    }
}
