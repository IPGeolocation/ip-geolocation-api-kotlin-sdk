package io.ipgeolocation.sdk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Constructor
import java.time.Duration

class JvmInteropCoverageTest {
    @Test
    fun jvmOverloadConstructorsRemainCallableFromJava() {
        val configType = IpGeolocationClientConfig::class.java
        val config0 = invokePublicConstructor(configType)
        val config1 = invokePublicConstructor(configType, "test-key")
        val config2 = invokePublicConstructor(configType, "test-key", "https://app.example.com")
        val config3 = invokePublicConstructor(configType, "test-key", "https://app.example.com", "https://api.example.com")
        val config4 = invokePublicConstructor(
            configType,
            "test-key",
            "https://app.example.com",
            "https://api.example.com",
            Duration.ofSeconds(2),
        )
        val config5 = invokePublicConstructor(
            configType,
            "test-key",
            "https://app.example.com",
            "https://api.example.com",
            Duration.ofSeconds(2),
            Duration.ofSeconds(5),
        )
        val config6 = invokePublicConstructor(
            configType,
            "test-key",
            "https://app.example.com",
            "https://api.example.com",
            Duration.ofSeconds(2),
            Duration.ofSeconds(5),
            4096,
        )

        assertThat(config0.apiKey).isNull()
        assertThat(config1.apiKey).isEqualTo("test-key")
        assertThat(config2.requestOrigin).isEqualTo("https://app.example.com")
        assertThat(config3.baseUrl).isEqualTo("https://api.example.com")
        assertThat(config4.connectTimeout).isEqualTo(Duration.ofSeconds(2))
        assertThat(config5.readTimeout).isEqualTo(Duration.ofSeconds(5))
        assertThat(config6.maxResponseBodyChars).isEqualTo(4096)

        val lookupType = LookupIpGeolocationRequest::class.java
        val lookup0 = invokePublicConstructor(lookupType)
        val lookup1 = invokePublicConstructor(lookupType, "8.8.8.8")
        val lookup2 = invokePublicConstructor(lookupType, "8.8.8.8", Language.EN)
        val lookup3 = invokePublicConstructor(lookupType, "8.8.8.8", Language.EN, listOf("security"))
        val lookup4 = invokePublicConstructor(
            lookupType,
            "8.8.8.8",
            Language.EN,
            listOf("security"),
            listOf("location.country_name"),
        )
        val lookup5 = invokePublicConstructor(
            lookupType,
            "8.8.8.8",
            Language.EN,
            listOf("security"),
            listOf("location.country_name"),
            listOf("currency"),
        )
        val lookup6 = invokePublicConstructor(
            lookupType,
            "8.8.8.8",
            Language.EN,
            listOf("security"),
            listOf("location.country_name"),
            listOf("currency"),
            "TestAgent/1.0",
        )
        val lookup7 = invokePublicConstructor(
            lookupType,
            "8.8.8.8",
            Language.EN,
            listOf("security"),
            listOf("location.country_name"),
            listOf("currency"),
            "TestAgent/1.0",
            mapOf("X-Test" to "value"),
        )
        val lookup8 = invokePublicConstructor(
            lookupType,
            "8.8.8.8",
            Language.EN,
            listOf("security"),
            listOf("location.country_name"),
            listOf("currency"),
            "TestAgent/1.0",
            mapOf("X-Test" to "value"),
            ResponseFormat.XML,
        )

        assertThat(lookup0.ip).isNull()
        assertThat(lookup1.ip).isEqualTo("8.8.8.8")
        assertThat(lookup2.lang).isEqualTo(Language.EN)
        assertThat(lookup3.include).containsExactly("security")
        assertThat(lookup4.fields).containsExactly("location.country_name")
        assertThat(lookup5.excludes).containsExactly("currency")
        assertThat(lookup6.userAgent).isEqualTo("TestAgent/1.0")
        assertThat(lookup7.headers).containsEntry("X-Test", "value")
        assertThat(lookup8.output).isEqualTo(ResponseFormat.XML)

        val bulkType = BulkLookupIpGeolocationRequest::class.java
        val bulk1 = invokePublicConstructor(bulkType, listOf("8.8.8.8"))
        val bulk2 = invokePublicConstructor(bulkType, listOf("8.8.8.8"), Language.DE)
        val bulk3 = invokePublicConstructor(bulkType, listOf("8.8.8.8"), Language.DE, listOf("security"))
        val bulk4 = invokePublicConstructor(
            bulkType,
            listOf("8.8.8.8"),
            Language.DE,
            listOf("security"),
            listOf("location.country_name"),
        )
        val bulk5 = invokePublicConstructor(
            bulkType,
            listOf("8.8.8.8"),
            Language.DE,
            listOf("security"),
            listOf("location.country_name"),
            listOf("currency"),
        )
        val bulk6 = invokePublicConstructor(
            bulkType,
            listOf("8.8.8.8"),
            Language.DE,
            listOf("security"),
            listOf("location.country_name"),
            listOf("currency"),
            "TestAgent/1.0",
        )
        val bulk7 = invokePublicConstructor(
            bulkType,
            listOf("8.8.8.8"),
            Language.DE,
            listOf("security"),
            listOf("location.country_name"),
            listOf("currency"),
            "TestAgent/1.0",
            mapOf("X-Test" to "value"),
        )
        val bulk8 = invokePublicConstructor(
            bulkType,
            listOf("8.8.8.8"),
            Language.DE,
            listOf("security"),
            listOf("location.country_name"),
            listOf("currency"),
            "TestAgent/1.0",
            mapOf("X-Test" to "value"),
            ResponseFormat.XML,
        )

        assertThat(bulk1.ips).containsExactly("8.8.8.8")
        assertThat(bulk2.lang).isEqualTo(Language.DE)
        assertThat(bulk3.include).containsExactly("security")
        assertThat(bulk4.fields).containsExactly("location.country_name")
        assertThat(bulk5.excludes).containsExactly("currency")
        assertThat(bulk6.userAgent).isEqualTo("TestAgent/1.0")
        assertThat(bulk7.headers).containsEntry("X-Test", "value")
        assertThat(bulk8.output).isEqualTo(ResponseFormat.XML)

        assertThat(ResponseFormat.values()).containsExactly(ResponseFormat.JSON, ResponseFormat.XML)
        assertThat(ResponseFormat.valueOf("XML")).isEqualTo(ResponseFormat.XML)
    }

    private fun <T> invokePublicConstructor(type: Class<T>, vararg args: Any?): T {
        val constructor = type.constructors
            .singleOrNull { it.parameterCount == args.size }
            ?: error("Expected one public constructor on ${type.name} with ${args.size} parameters")
        @Suppress("UNCHECKED_CAST")
        return (constructor as Constructor<T>).newInstance(*args)
    }
}
