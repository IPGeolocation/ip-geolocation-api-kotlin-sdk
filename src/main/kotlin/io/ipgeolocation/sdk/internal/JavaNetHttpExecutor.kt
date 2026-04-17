package io.ipgeolocation.sdk.internal

import io.ipgeolocation.sdk.IpGeolocationClientConfig
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration

internal class JavaNetHttpExecutor(
    connectTimeout: Duration,
    private val maxResponseBodyChars: Int = DEFAULT_MAX_RESPONSE_BODY_CHARS,
    private val connectionFactory: ConnectionFactory = DefaultConnectionFactory(),
) : HttpExecutor {
    private val connectTimeoutMillis: Int = toMillis(connectTimeout)

    init {
        require(!connectTimeout.isZero && !connectTimeout.isNegative) { "connectTimeout must be greater than zero" }
        require(maxResponseBodyChars > 0) { "maxResponseBodyChars must be greater than zero" }
    }

    override fun send(request: HttpRequestData): HttpResponseData {
        val connection = connectionFactory.open(request.url)
        var keepAliveEligible = false
        try {
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = toMillis(request.timeout)
            connection.requestMethod = request.method
            applyHeaders(connection, request.headers)
            writeBodyIfPresent(connection, request)
            val statusCode = connection.responseCode
            val response = HttpResponseData(
                statusCode = statusCode,
                body = readBody(connection, statusCode),
                headers = normalizeHeaderFields(connection.headerFields),
            )
            keepAliveEligible = true
            return response
        } finally {
            if (!keepAliveEligible) {
                connection.disconnect()
            }
        }
    }

    private fun applyHeaders(connection: HttpURLConnection, headers: Map<String, List<String>>) {
        for ((name, values) in headers) {
            for (value in values) {
                connection.addRequestProperty(name, value)
            }
        }
    }

    private fun writeBodyIfPresent(connection: HttpURLConnection, request: HttpRequestData) {
        if (request.method.uppercase() !in setOf("POST", "PUT")) {
            return
        }
        val body = request.body ?: return
        val payload = body.toByteArray(StandardCharsets.UTF_8)
        connection.doOutput = true
        if (connection.getRequestProperty("Content-Length") == null) {
            connection.setFixedLengthStreamingMode(payload.size)
        }
        connection.outputStream.use { output ->
            output.write(payload)
            output.flush()
        }
    }

    private fun readBody(connection: HttpURLConnection, statusCode: Int): String {
        val stream = if (statusCode >= 400) connection.errorStream else connection.inputStream
        if (stream == null) {
            return ""
        }
        return readStream(stream)
    }

    private fun readStream(stream: InputStream): String {
        BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
            val body = StringBuilder()
            val buffer = CharArray(2048)
            var totalChars = 0
            while (true) {
                val read = reader.read(buffer)
                if (read == -1) {
                    break
                }
                totalChars += read
                if (totalChars > maxResponseBodyChars) {
                    throw IOException("Response body exceeded max size of $maxResponseBodyChars characters")
                }
                body.append(buffer, 0, read)
            }
            return body.toString()
        }
    }

    private fun normalizeHeaderFields(headers: Map<String?, List<String>?>): Map<String, List<String>> {
        if (headers.isEmpty()) {
            return emptyMap()
        }
        val normalized = LinkedHashMap<String, List<String>>()
        for ((name, values) in headers) {
            if (name.isNullOrBlank()) {
                continue
            }
            normalized[name] = values?.toList() ?: emptyList()
        }
        return normalized.toMap()
    }

    private fun toMillis(duration: Duration): Int {
        val millis = duration.toMillis()
        return if (millis > Int.MAX_VALUE) Int.MAX_VALUE else millis.toInt()
    }

    internal fun interface ConnectionFactory {
        fun open(uri: URI): HttpURLConnection
    }

    private class DefaultConnectionFactory : ConnectionFactory {
        override fun open(uri: URI): HttpURLConnection = uri.toURL().openConnection() as HttpURLConnection
    }

    companion object {
        const val DEFAULT_MAX_RESPONSE_BODY_CHARS: Int = IpGeolocationClientConfig.DEFAULT_MAX_RESPONSE_BODY_CHARS
    }
}
