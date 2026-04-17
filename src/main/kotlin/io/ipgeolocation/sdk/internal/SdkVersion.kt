package io.ipgeolocation.sdk.internal

import java.util.Properties

internal object SdkVersion {
    val VERSION: String = loadVersion()

    private fun loadVersion(): String {
        return SdkVersion::class.java.getResourceAsStream("/io/ipgeolocation/sdk/version.properties")?.use { input ->
            val properties = Properties()
            properties.load(input)
            properties.getProperty("sdk.version", "dev")
        } ?: "dev"
    }
}
