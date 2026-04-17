package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.ValidationException
import io.ipgeolocation.sdk.internal.containsCrOrLf
import io.ipgeolocation.sdk.internal.normalizeHeaders
import io.ipgeolocation.sdk.internal.normalizeOptionalHeaderValue
import io.ipgeolocation.sdk.internal.normalizeTokenList

data class LookupIpGeolocationRequest @JvmOverloads constructor(
    val ip: String? = null,
    val lang: Language? = null,
    val include: List<String> = emptyList(),
    val fields: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val userAgent: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val output: ResponseFormat = ResponseFormat.JSON,
) {
    fun validate() {
        normalizeLookupRequest(this)
    }
}

internal data class NormalizedLookupRequest(
    val ip: String?,
    val lang: String?,
    val include: List<String>,
    val fields: List<String>,
    val excludes: List<String>,
    val userAgent: String?,
    val headers: Map<String, String>,
    val output: ResponseFormat,
)

internal fun normalizeLookupRequest(request: LookupIpGeolocationRequest): NormalizedLookupRequest {
    return NormalizedLookupRequest(
        ip = normalizeLookupIp(request.ip),
        lang = request.lang?.code,
        include = normalizeTokenList(request.include, "include"),
        fields = normalizeTokenList(request.fields, "fields"),
        excludes = normalizeTokenList(request.excludes, "excludes"),
        userAgent = normalizeOptionalHeaderValue(request.userAgent, "userAgent"),
        headers = normalizeHeaders(request.headers),
        output = request.output,
    )
}

internal fun normalizeLookupIp(value: String?): String? {
    if (value == null) {
        return null
    }
    val normalized = value.trim()
    if (normalized.isEmpty()) {
        return null
    }
    if (containsCrOrLf(normalized)) {
        throw ValidationException("ip must not contain CR or LF")
    }
    return normalized
}
