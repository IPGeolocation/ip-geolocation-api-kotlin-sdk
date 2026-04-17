package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.internal.HttpExecutor
import io.ipgeolocation.sdk.internal.HttpRequestData
import io.ipgeolocation.sdk.internal.HttpResponseData
import java.time.Duration
import java.util.ArrayDeque

internal class TestHttpExecutor : HttpExecutor {
    private val scripted = ArrayDeque<Any>()
    val capturedRequests: MutableList<HttpRequestData> = mutableListOf()
    val capturedTimeouts: MutableList<Duration> = mutableListOf()
    var closed: Boolean = false
        private set

    fun enqueueResponse(
        statusCode: Int,
        body: String,
        headers: Map<String, List<String>> = emptyMap(),
    ) {
        scripted.addLast(HttpResponseData(statusCode = statusCode, body = body, headers = headers))
    }

    fun enqueueThrowable(throwable: Throwable) {
        scripted.addLast(throwable)
    }

    override fun send(request: HttpRequestData): HttpResponseData {
        capturedRequests += request
        capturedTimeouts += request.timeout
        if (scripted.isEmpty()) {
            error("No scripted response configured")
        }
        val next = scripted.removeFirst()
        return when (next) {
            is Throwable -> throw next
            is HttpResponseData -> next
            else -> error("Unsupported scripted value: ${next::class.qualifiedName}")
        }
    }

    override fun close() {
        closed = true
    }
}
