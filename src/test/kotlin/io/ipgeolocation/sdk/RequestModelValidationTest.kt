package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class RequestModelValidationTest {
    @Test
    fun lookupRequestValidationNormalizesInput() {
        val request = LookupIpGeolocationRequest(
            ip = " 8.8.8.8 ",
            lang = Language.DE,
            include = listOf("security", " security ", "abuse"),
            fields = listOf(" location.country_name "),
            excludes = listOf(" currency "),
            userAgent = " TestAgent/1.0 ",
            headers = linkedMapOf(" X-Test " to " value "),
            output = ResponseFormat.JSON,
        )

        request.validate()
        val normalized = normalizeLookupRequest(request)

        assertThat(normalized.ip).isEqualTo("8.8.8.8")
        assertThat(normalized.lang).isEqualTo("de")
        assertThat(normalized.include).containsExactly("security", "abuse")
        assertThat(normalized.fields).containsExactly("location.country_name")
        assertThat(normalized.excludes).containsExactly("currency")
        assertThat(normalized.userAgent).isEqualTo("TestAgent/1.0")
        assertThat(normalized.headers).containsEntry("X-Test", "value")
        assertThat(normalizeLookupIp("   ")).isNull()
    }

    @Test
    fun lookupRequestValidationRejectsInvalidTokensHeadersAndIp() {
        assertThatThrownBy {
            normalizeLookupRequest(LookupIpGeolocationRequest(include = listOf(" ")))
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("include")

        assertThatThrownBy {
            normalizeLookupRequest(LookupIpGeolocationRequest(userAgent = " "))
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("userAgent")

        assertThatThrownBy {
            normalizeLookupRequest(LookupIpGeolocationRequest(headers = mapOf(" " to "value")))
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("blank names")

        assertThatThrownBy { normalizeLookupIp("8.8.8.8\r\nx") }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("CR or LF")
    }

    @Test
    fun bulkRequestValidationEnforcesLimitsAndNormalizesEntries() {
        val request = BulkLookupIpGeolocationRequest(
            ips = listOf(" 8.8.8.8 ", "1.1.1.1"),
            include = listOf("security", "security"),
            headers = mapOf(" X-Test " to " value "),
        )

        request.validate()
        val normalized = normalizeBulkLookupRequest(request)

        assertThat(normalized.ips).containsExactly("8.8.8.8", "1.1.1.1")
        assertThat(normalized.include).containsExactly("security")
        assertThat(normalized.headers).containsEntry("X-Test", "value")

        assertThatThrownBy {
            BulkLookupIpGeolocationRequest(ips = emptyList()).validate()
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("ips must not be empty")

        assertThatThrownBy {
            BulkLookupIpGeolocationRequest(ips = List(50_001) { "8.8.8.8" }).validate()
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("50000")

        assertThatThrownBy {
            normalizeBulkLookupRequest(BulkLookupIpGeolocationRequest(ips = listOf(" ")))
        }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("blank values")
    }
}
