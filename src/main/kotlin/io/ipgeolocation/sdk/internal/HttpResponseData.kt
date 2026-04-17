package io.ipgeolocation.sdk.internal

internal data class HttpResponseData(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, List<String>>,
)

