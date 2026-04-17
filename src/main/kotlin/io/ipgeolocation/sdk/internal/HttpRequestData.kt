package io.ipgeolocation.sdk.internal

import java.net.URI
import java.time.Duration

internal data class HttpRequestData(
    val url: URI,
    val method: String,
    val headers: Map<String, List<String>>,
    val body: String? = null,
    val timeout: Duration,
)

