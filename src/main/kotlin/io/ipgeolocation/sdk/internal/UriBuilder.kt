package io.ipgeolocation.sdk.internal

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object UriBuilder {
    fun build(
        baseUrl: String,
        path: String,
        queryParams: Map<String, String?>,
    ): URI {
        val query = queryParams.entries
            .mapNotNull { (key, value) ->
                val normalizedKey = key.trim()
                val normalizedValue = value?.trim()
                if (normalizedKey.isEmpty() || normalizedValue.isNullOrEmpty()) {
                    null
                } else {
                    encode(normalizedKey) + "=" + encode(normalizedValue)
                }
            }
            .joinToString("&")

        val url = StringBuilder(baseUrl).append(path)
        if (query.isNotEmpty()) {
            url.append('?').append(query)
        }
        return URI(url.toString())
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
