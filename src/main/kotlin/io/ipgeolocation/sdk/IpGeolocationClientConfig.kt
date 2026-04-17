package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.ValidationException
import java.net.URI
import java.time.Duration

class IpGeolocationClientConfig @JvmOverloads constructor(
    apiKey: String? = null,
    requestOrigin: String? = null,
    baseUrl: String = DEFAULT_BASE_URL,
    connectTimeout: Duration = DEFAULT_CONNECT_TIMEOUT,
    readTimeout: Duration = DEFAULT_READ_TIMEOUT,
    maxResponseBodyChars: Int = DEFAULT_MAX_RESPONSE_BODY_CHARS,
) {
    val apiKey: String? = normalizeApiKey(apiKey)
    val requestOrigin: String? = normalizeRequestOrigin(requestOrigin)
    val baseUrl: String = normalizeBaseUrl(baseUrl)
    val connectTimeout: Duration = normalizeTimeout(connectTimeout, "connectTimeout")
    val readTimeout: Duration = normalizeTimeout(readTimeout, "readTimeout")
    val maxResponseBodyChars: Int = normalizeMaxResponseBodyChars(maxResponseBodyChars)

    init {
        if (this.connectTimeout > this.readTimeout) {
            throw ValidationException("connectTimeout must be <= readTimeout")
        }
    }

    fun safeMap(): Map<String, Any?> {
        return linkedMapOf(
            "apiKey" to if (apiKey == null) null else "[REDACTED]",
            "requestOrigin" to requestOrigin,
            "baseUrl" to baseUrl,
            "connectTimeoutMs" to connectTimeout.toMillis(),
            "readTimeoutMs" to readTimeout.toMillis(),
            "maxResponseBodyChars" to maxResponseBodyChars,
        )
    }

    override fun toString(): String = "IpGeolocationClientConfig(${safeMap()})"

    companion object {
        const val DEFAULT_BASE_URL: String = "https://api.ipgeolocation.io"
        val DEFAULT_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
        val DEFAULT_READ_TIMEOUT: Duration = Duration.ofSeconds(30)
        const val DEFAULT_MAX_RESPONSE_BODY_CHARS: Int = 32 * 1024 * 1024

        private fun normalizeApiKey(value: String?): String? {
            if (value == null) {
                return null
            }
            val normalized = value.trim()
            if (normalized.isEmpty()) {
                throw ValidationException("apiKey must not be blank")
            }
            return normalized
        }

        private fun normalizeRequestOrigin(value: String?): String? {
            if (value == null) {
                return null
            }
            val normalized = value.trim()
            if (normalized.isEmpty()) {
                throw ValidationException("requestOrigin must not be blank")
            }
            if (normalized.contains('\r') || normalized.contains('\n')) {
                throw ValidationException("requestOrigin must not contain CR or LF")
            }

            val uri = try {
                URI(normalized)
            } catch (_: Exception) {
                throw ValidationException("requestOrigin must be an absolute http or https origin")
            }

            if (!uri.isAbsolute || (uri.scheme != "http" && uri.scheme != "https")) {
                throw ValidationException("requestOrigin must be an absolute http or https origin")
            }
            if (uri.userInfo != null) {
                throw ValidationException("requestOrigin must not include userinfo")
            }
            if (uri.rawQuery != null || uri.fragment != null) {
                throw ValidationException("requestOrigin must not include query or fragment")
            }
            if (uri.host.isNullOrBlank()) {
                throw ValidationException("requestOrigin must include a valid host")
            }
            if (!uri.path.isNullOrEmpty() && uri.path != "/") {
                throw ValidationException("requestOrigin must not include a path")
            }

            val portPart = if (uri.port == -1) "" else ":${uri.port}"
            return "${uri.scheme}://${uri.host}$portPart"
        }

        private fun normalizeBaseUrl(value: String): String {
            val normalized = value.trim().trimEnd('/')
            if (normalized.isEmpty()) {
                throw ValidationException("baseUrl must not be blank")
            }

            val uri = try {
                URI(normalized)
            } catch (_: Exception) {
                throw ValidationException("baseUrl must be an absolute http or https URL")
            }

            if (!uri.isAbsolute || (uri.scheme != "http" && uri.scheme != "https")) {
                throw ValidationException("baseUrl must be an absolute http or https URL")
            }
            if (uri.userInfo != null) {
                throw ValidationException("baseUrl must not include userinfo")
            }
            if (uri.rawQuery != null || uri.fragment != null) {
                throw ValidationException("baseUrl must not include query or fragment")
            }
            if (uri.host.isNullOrBlank()) {
                throw ValidationException("baseUrl must include a valid host")
            }
            return normalized
        }

        private fun normalizeTimeout(value: Duration, fieldName: String): Duration {
            if (value.isZero || value.isNegative) {
                throw ValidationException("$fieldName must be greater than zero")
            }
            return value
        }

        private fun normalizeMaxResponseBodyChars(value: Int): Int {
            if (value <= 0) {
                throw ValidationException("maxResponseBodyChars must be greater than zero")
            }
            return value
        }
    }
}
