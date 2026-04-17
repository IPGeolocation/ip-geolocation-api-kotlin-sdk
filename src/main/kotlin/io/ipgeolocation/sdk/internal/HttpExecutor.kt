package io.ipgeolocation.sdk.internal

import java.io.Closeable

internal interface HttpExecutor : Closeable {
    fun send(request: HttpRequestData): HttpResponseData

    override fun close() {
        // no-op by default
    }
}

