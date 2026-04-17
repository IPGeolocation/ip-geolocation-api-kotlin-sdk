package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.SerializationException
import io.ipgeolocation.sdk.model.BulkLookupResult
import io.ipgeolocation.sdk.model.CountryMetadata
import io.ipgeolocation.sdk.model.IpGeolocationResponse
import io.ipgeolocation.sdk.model.Location
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class JsonOutputTest {
    @Test
    fun compactModeOmitsNullFields() {
        val json = JsonOutput.toJson(sampleResponse(), JsonOutputMode.COMPACT)

        assertThat(json).contains(""""ip":"8.8.8.8"""")
        assertThat(json).contains(""""country_name":"United States"""")
        assertThat(json).contains(""""country_metadata"""")
        assertThat(json).doesNotContain(""""security":null""")
        assertThat(json).doesNotContain(""""domain":null""")
    }

    @Test
    fun fullModeIncludesNullFields() {
        val json = JsonOutput.toJson(sampleResponse(), JsonOutputMode.FULL)

        assertThat(json).contains(""""security":null""")
        assertThat(json).contains(""""domain":null""")
        assertThat(json).contains(""""time_zone":null""")
    }

    @Test
    fun prettyJsonUsesCompactModeByDefault() {
        val json = JsonOutput.toPrettyJson(sampleResponse())

        assertThat(json).contains("\n")
        assertThat(json).doesNotContain(""""security" : null""")
    }

    @Test
    fun bulkLookupResultJsonDoesNotLeakSyntheticSuccessProperty() {
        val json = JsonOutput.toJson(BulkLookupResult(data = IpGeolocationResponse(ip = "8.8.8.8")))

        assertThat(json).contains(""""data":{"ip":"8.8.8.8"}""")
        assertThat(json).doesNotContain(""""success"""")
    }

    @Test
    fun serializationFailuresMapToSerializationException() {
        val recursive = linkedMapOf<String, Any>()
        recursive["self"] = recursive

        assertThatThrownBy { JsonOutput.toJson(recursive) }
            .isInstanceOf(SerializationException::class.java)
            .hasMessageContaining("Failed to serialize output as JSON")
    }

    private fun sampleResponse(): IpGeolocationResponse {
        return IpGeolocationResponse(
            ip = "8.8.8.8",
            location = Location(
                countryName = "United States",
                city = "Mountain View",
            ),
            countryMetadata = CountryMetadata(
                callingCode = "+1",
                tld = ".us",
                languages = listOf("en-US"),
            ),
        )
    }
}
