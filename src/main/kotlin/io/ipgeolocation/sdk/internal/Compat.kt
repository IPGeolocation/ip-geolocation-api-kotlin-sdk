package io.ipgeolocation.sdk.internal

import io.ipgeolocation.sdk.exceptions.ValidationException

internal fun containsCrOrLf(value: String): Boolean = value.contains('\r') || value.contains('\n')

internal fun normalizeOptionalHeaderValue(value: String?, fieldName: String): String? {
    if (value == null) {
        return null
    }
    val normalized = value.trim()
    if (normalized.isEmpty()) {
        throw ValidationException("$fieldName must not be blank")
    }
    if (containsCrOrLf(normalized)) {
        throw ValidationException("$fieldName must not contain CR or LF")
    }
    return normalized
}

internal fun normalizeTokenList(values: List<String>, fieldName: String): List<String> {
    if (values.isEmpty()) {
        return emptyList()
    }
    val normalized = LinkedHashSet<String>(values.size)
    for (value in values) {
        val token = value.trim()
        if (token.isEmpty()) {
            throw ValidationException("$fieldName value must not be blank")
        }
        normalized += token
    }
    return normalized.toList()
}

internal fun normalizeHeaders(headers: Map<String, String>): Map<String, String> {
    if (headers.isEmpty()) {
        return emptyMap()
    }
    val normalized = LinkedHashMap<String, String>(headers.size)
    for ((rawName, rawValue) in headers) {
        val name = rawName.trim()
        val value = rawValue.trim()
        if (name.isEmpty()) {
            throw ValidationException("headers must not contain blank names")
        }
        if (value.isEmpty()) {
            throw ValidationException("headers must not contain blank values")
        }
        if (containsCrOrLf(name) || containsCrOrLf(value)) {
            throw ValidationException("headers must not contain CR or LF")
        }
        normalized[name] = value
    }
    return normalized.toMap()
}

internal fun mergeHeaders(vararg maps: Map<String, List<String>>?): Map<String, List<String>> {
    val merged = LinkedHashMap<String, List<String>>()
    val namesByLower = HashMap<String, String>()
    for (headerMap in maps) {
        if (headerMap == null) {
            continue
        }
        for ((rawName, rawValues) in headerMap) {
            val name = rawName.trim()
            if (name.isEmpty()) {
                continue
            }
            val existingName = namesByLower[name.lowercase()]
            if (existingName != null && existingName != name) {
                merged.remove(existingName)
            }
            namesByLower[name.lowercase()] = name
            merged[name] = rawValues.filterNotNull().toList()
        }
    }
    return merged.toMap()
}

internal fun resolveUserAgentHeader(
    userAgent: String?,
    headers: Map<String, String>,
    defaultUserAgent: String,
): String {
    if (userAgent != null) {
        return userAgent
    }
    return headers.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.value ?: defaultUserAgent
}

