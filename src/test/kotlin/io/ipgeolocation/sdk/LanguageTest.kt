package io.ipgeolocation.sdk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LanguageTest {
    @Test
    fun fromCodeHandlesCaseAndUnknownValues() {
        assertThat(Language.fromCode("EN")).isEqualTo(Language.EN)
        assertThat(Language.fromCode("de")).isEqualTo(Language.DE)
        assertThat(Language.fromCode("unknown")).isNull()
    }

    @Test
    fun enumsExposeExpectedWireValues() {
        assertThat(ResponseFormat.JSON.wireValue).isEqualTo("json")
        assertThat(ResponseFormat.XML.wireValue).isEqualTo("xml")
        assertThat(ResponseFormat.valueOf("JSON")).isEqualTo(ResponseFormat.JSON)
        assertThat(JsonOutputMode.entries).containsExactly(JsonOutputMode.COMPACT, JsonOutputMode.FULL)
        assertThat(Language.entries).contains(Language.FR, Language.PT)
    }
}
