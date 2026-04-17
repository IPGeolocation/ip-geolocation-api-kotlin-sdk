package io.ipgeolocation.sdk

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ApiResponseMetadataTest {
    @Test
    fun normalizesHeadersAndSupportsCaseInsensitiveLookup() {
        val rawHeaders = linkedMapOf(
            "X-Trace-Id" to listOf("trace-1"),
            " " to listOf("ignored"),
            "X-Empty" to emptyList(),
        )

        val metadata = ApiResponseMetadata(
            creditsCharged = 1,
            successfulRecords = 2,
            statusCode = 200,
            durationMs = 15,
            rawHeaders = rawHeaders,
        )

        rawHeaders["X-Trace-Id"] = listOf("mutated")

        assertThat(metadata.rawHeaders).containsKeys("X-Trace-Id", "X-Empty")
        assertThat(metadata.rawHeaders).doesNotContainKey(" ")
        assertThat(metadata.firstHeaderValue("x-trace-id")).isEqualTo("trace-1")
        assertThat(metadata.headerValues("X-Empty")).isEmpty()
        assertThat(metadata.firstHeaderValue("missing")).isNull()
    }

    @Test
    fun rejectsInvalidInputsAndBlankHeaderNames() {
        assertThatThrownBy {
            ApiResponseMetadata(statusCode = 99, durationMs = 0)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("statusCode")

        assertThatThrownBy {
            ApiResponseMetadata(statusCode = 200, durationMs = -1)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("durationMs")

        val metadata = ApiResponseMetadata(statusCode = 200, durationMs = 0)
        assertThatThrownBy { metadata.headerValues(" ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("header name")
    }

    @Test
    fun equalityHashCodeAndToStringUseNormalizedHeaders() {
        val left = ApiResponseMetadata(
            creditsCharged = 3,
            successfulRecords = 1,
            statusCode = 202,
            durationMs = 9,
            rawHeaders = linkedMapOf(
                "X-Trace-Id" to listOf("trace-1"),
                " " to listOf("ignored"),
            ),
        )
        val right = ApiResponseMetadata(
            creditsCharged = 3,
            successfulRecords = 1,
            statusCode = 202,
            durationMs = 9,
            rawHeaders = linkedMapOf("X-Trace-Id" to listOf("trace-1")),
        )

        assertThat(left).isEqualTo(right)
        assertThat(left).isEqualTo(left)
        assertThat(left).isNotEqualTo(null)
        assertThat(left).isNotEqualTo("other")
        assertThat(left.hashCode()).isEqualTo(right.hashCode())
        assertThat(left.toString()).contains("statusCode=202", "X-Trace-Id")

        val empty = ApiResponseMetadata(statusCode = 204, durationMs = 0, rawHeaders = emptyMap())
        assertThat(empty.rawHeaders).isEmpty()
    }
}
