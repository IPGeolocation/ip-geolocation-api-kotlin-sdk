package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.ValidationException
import io.ipgeolocation.sdk.internal.normalizeHeaders
import io.ipgeolocation.sdk.internal.normalizeOptionalHeaderValue
import io.ipgeolocation.sdk.internal.normalizeTokenList

data class BulkLookupIpGeolocationRequest @JvmOverloads constructor(
    val ips: List<String>,
    val lang: Language? = null,
    val include: List<String> = emptyList(),
    val fields: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val userAgent: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val output: ResponseFormat = ResponseFormat.JSON,
) {
    fun validate() {
        normalizeBulkLookupRequest(this)
    }
}

internal data class NormalizedBulkLookupRequest(
    val ips: List<String>,
    val lang: String?,
    val include: List<String>,
    val fields: List<String>,
    val excludes: List<String>,
    val userAgent: String?,
    val headers: Map<String, String>,
    val output: ResponseFormat,
)

internal fun normalizeBulkLookupRequest(request: BulkLookupIpGeolocationRequest): NormalizedBulkLookupRequest {
    if (request.ips.isEmpty()) {
        throw ValidationException("ips must not be empty")
    }
    if (request.ips.size > 50_000) {
        throw ValidationException("ips must contain at most 50000 entries")
    }

    return NormalizedBulkLookupRequest(
        ips = request.ips.map {
            normalizeLookupIp(it) ?: throw ValidationException("ips must not contain blank values")
        },
        lang = request.lang?.code,
        include = normalizeTokenList(request.include, "include"),
        fields = normalizeTokenList(request.fields, "fields"),
        excludes = normalizeTokenList(request.excludes, "excludes"),
        userAgent = normalizeOptionalHeaderValue(request.userAgent, "userAgent"),
        headers = normalizeHeaders(request.headers),
        output = request.output,
    )
}
