package io.ipgeolocation.sdk.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UriBuilderTest {
    @Test
    fun omitsBlankNamesAndValues() {
        val uri = UriBuilder.build(
            baseUrl = "https://api.ipgeolocation.io",
            path = "/v3/ipgeo",
            queryParams = linkedMapOf(
                "apiKey" to " ",
                " " to "value",
                "lang" to "en",
            ),
        )

        assertThat(uri.toString()).isEqualTo("https://api.ipgeolocation.io/v3/ipgeo?lang=en")
    }

    @Test
    fun encodesReservedCharactersAndKeepsBasePathWithoutQuery() {
        val encoded = UriBuilder.build(
            baseUrl = "https://api.ipgeolocation.io",
            path = "/v3/ipgeo",
            queryParams = linkedMapOf(
                "include" to "security,abuse",
                "ip" to "New York",
            ),
        )
        val plain = UriBuilder.build(
            baseUrl = "https://api.ipgeolocation.io",
            path = "/v3/ipgeo",
            queryParams = emptyMap(),
        )

        assertThat(encoded.toString())
            .isEqualTo("https://api.ipgeolocation.io/v3/ipgeo?include=security%2Cabuse&ip=New+York")
        assertThat(plain.toString()).isEqualTo("https://api.ipgeolocation.io/v3/ipgeo")
    }
}
