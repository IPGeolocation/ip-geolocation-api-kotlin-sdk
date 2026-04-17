package io.ipgeolocation.sdk

class ApiResponseMetadata(
    val creditsCharged: Int? = null,
    val successfulRecords: Int? = null,
    val statusCode: Int,
    val durationMs: Long,
    rawHeaders: Map<String, List<String>> = emptyMap(),
) {
    val rawHeaders: Map<String, List<String>> = normalizeHeaders(rawHeaders)

    init {
        require(statusCode in 100..599) { "statusCode must be between 100 and 599" }
        require(durationMs >= 0) { "durationMs must be >= 0" }
    }

    fun headerValues(name: String): List<String> {
        require(name.isNotBlank()) { "header name must not be blank" }
        return rawHeaders.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value ?: emptyList()
    }

    fun firstHeaderValue(name: String): String? = headerValues(name).firstOrNull()

    private fun normalizeHeaders(headers: Map<String, List<String>>): Map<String, List<String>> {
        if (headers.isEmpty()) {
            return emptyMap()
        }

        val normalized = LinkedHashMap<String, List<String>>(headers.size)
        for ((name, values) in headers) {
            if (name.isBlank()) {
                continue
            }
            normalized[name] = values.toList()
        }
        return normalized.toMap()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ApiResponseMetadata) {
            return false
        }

        return creditsCharged == other.creditsCharged &&
            successfulRecords == other.successfulRecords &&
            statusCode == other.statusCode &&
            durationMs == other.durationMs &&
            rawHeaders == other.rawHeaders
    }

    override fun hashCode(): Int {
        var result = creditsCharged ?: 0
        result = 31 * result + (successfulRecords ?: 0)
        result = 31 * result + statusCode
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + rawHeaders.hashCode()
        return result
    }

    override fun toString(): String {
        return "ApiResponseMetadata(creditsCharged=$creditsCharged, successfulRecords=$successfulRecords, statusCode=$statusCode, durationMs=$durationMs, rawHeaders=$rawHeaders)"
    }
}

