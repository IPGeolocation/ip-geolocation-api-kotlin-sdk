package io.ipgeolocation.sdk.internal

import io.ipgeolocation.sdk.headers
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Duration

class JavaNetHttpExecutorTest {
    @Test
    fun rejectsInvalidConstructorArguments() {
        assertThatThrownBy { JavaNetHttpExecutor(Duration.ZERO) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("connectTimeout")

        assertThatThrownBy { JavaNetHttpExecutor(Duration.ofSeconds(-1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("connectTimeout")

        assertThatThrownBy { JavaNetHttpExecutor(Duration.ofSeconds(1), maxResponseBodyChars = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("maxResponseBodyChars")
    }

    @Test
    fun mapsResponseAndWritesRequestBody() {
        val connection = StubHttpURLConnection(
            URI.create("https://api.ipgeolocation.io/v3/ipgeo").toURL(),
            200,
            """{"ip":"8.8.8.8"}""",
            headers("X-Test", "a"),
        )
        val executor = JavaNetHttpExecutor(
            connectTimeout = Duration.ofSeconds(3),
            connectionFactory = JavaNetHttpExecutor.ConnectionFactory { connection },
        )

        val data = executor.send(
            HttpRequestData(
                url = URI.create("https://api.ipgeolocation.io/v3/ipgeo?ip=8.8.8.8"),
                method = "POST",
                headers = mapOf("User-Agent" to listOf("ipgeolocation-kotlin-sdk/test")),
                body = """{"ips":["8.8.8.8"]}""",
                timeout = Duration.ofSeconds(4),
            ),
        )

        assertThat(connection.requestMethod).isEqualTo("POST")
        assertThat(connection.connectTimeout).isEqualTo(3000)
        assertThat(connection.readTimeout).isEqualTo(4000)
        assertThat(connection.requestPropertyValues("User-Agent")).containsExactly("ipgeolocation-kotlin-sdk/test")
        assertThat(connection.writtenBody()).isEqualTo("""{"ips":["8.8.8.8"]}""")
        assertThat(data.statusCode).isEqualTo(200)
        assertThat(data.body).isEqualTo("""{"ip":"8.8.8.8"}""")
        assertThat(data.headers).containsEntry("X-Test", listOf("a"))
        assertThat(connection.disconnectCalls).isZero()
    }

    @Test
    fun usesErrorStreamSkipsGetBodiesAndRejectsOversizedResponses() {
        val errorConnection = StubHttpURLConnection(
            URI.create("https://api.ipgeolocation.io/v3/ipgeo").toURL(),
            401,
            """{"message":"invalid key"}""",
            emptyMap(),
        )
        val errorExecutor = JavaNetHttpExecutor(
            connectTimeout = Duration.ofSeconds(2),
            connectionFactory = JavaNetHttpExecutor.ConnectionFactory { errorConnection },
        )

        val errorResponse = errorExecutor.send(
            HttpRequestData(
                url = URI.create("https://api.ipgeolocation.io/v3/ipgeo"),
                method = "GET",
                headers = mapOf("X-Test" to listOf("value")),
                body = """{"ignored":true}""",
                timeout = Duration.ofDays(30_000),
            ),
        )

        assertThat(errorConnection.outputStreamRequested).isFalse()
        assertThat(errorConnection.fixedLengthStreamingModeValue).isEqualTo(-1)
        assertThat(errorConnection.readTimeout).isEqualTo(Int.MAX_VALUE)
        assertThat(errorResponse.statusCode).isEqualTo(401)
        assertThat(errorResponse.body).contains("invalid key")

        val oversizedConnection = StubHttpURLConnection(
            URI.create("https://api.ipgeolocation.io/v3/ipgeo").toURL(),
            200,
            """{"ip":"1234567890"}""",
            emptyMap(),
        )
        val limitedExecutor = JavaNetHttpExecutor(
            connectTimeout = Duration.ofSeconds(2),
            maxResponseBodyChars = 8,
            connectionFactory = JavaNetHttpExecutor.ConnectionFactory { oversizedConnection },
        )

        assertThatThrownBy {
            limitedExecutor.send(
                HttpRequestData(
                    url = URI.create("https://api.ipgeolocation.io/v3/ipgeo"),
                    method = "GET",
                    headers = emptyMap(),
                    timeout = Duration.ofSeconds(2),
                ),
            )
        }
            .isInstanceOf(IOException::class.java)
            .hasMessageContaining("Response body exceeded max size")
        assertThat(oversizedConnection.disconnectCalls).isEqualTo(1)
    }

    @Test
    fun honorsExistingContentLengthAndHandlesNullErrorStream() {
        val connection = StubHttpURLConnection(
            URI.create("https://api.ipgeolocation.io/v3/ipgeo-bulk").toURL(),
            500,
            """{"message":"upstream failed"}""",
            emptyMap(),
        ).apply {
            existingRequestProperties["Content-Length"] = "4"
            nullErrorStream = true
        }

        val executor = JavaNetHttpExecutor(
            connectTimeout = Duration.ofDays(30_000),
            connectionFactory = JavaNetHttpExecutor.ConnectionFactory { connection },
        )

        val response = executor.send(
            HttpRequestData(
                url = URI.create("https://api.ipgeolocation.io/v3/ipgeo-bulk"),
                method = "PUT",
                headers = emptyMap(),
                body = """{"ips":["8.8.8.8"]}""",
                timeout = Duration.ofDays(30_000),
            ),
        )

        assertThat(connection.fixedLengthStreamingModeValue).isEqualTo(-1)
        assertThat(connection.writtenBody()).isEqualTo("""{"ips":["8.8.8.8"]}""")
        assertThat(response.statusCode).isEqualTo(500)
        assertThat(response.body).isEmpty()
        assertThat(connection.disconnectCalls).isZero()
    }

    @Test
    fun putWithoutBodySkipsOutputAndHeaderNormalizationDropsBlankEntries() {
        val connection = StubHttpURLConnection(
            URI.create("https://api.ipgeolocation.io/v3/ipgeo-bulk").toURL(),
            200,
            """{"ok":true}""",
            mapOf(
                "" to listOf("ignored"),
                "X-Null" to null,
                "X-Test" to listOf("value"),
            ),
        )
        val executor = JavaNetHttpExecutor(
            connectTimeout = Duration.ofSeconds(2),
            connectionFactory = JavaNetHttpExecutor.ConnectionFactory { connection },
        )

        val response = executor.send(
            HttpRequestData(
                url = URI.create("https://api.ipgeolocation.io/v3/ipgeo-bulk"),
                method = "PUT",
                headers = emptyMap(),
                body = null,
                timeout = Duration.ofSeconds(2),
            ),
        )

        assertThat(connection.outputStreamRequested).isFalse()
        assertThat(response.headers).doesNotContainKey("")
        assertThat(response.headers["X-Null"]).isEmpty()
        assertThat(response.headers["X-Test"]).containsExactly("value")
        assertThat(connection.disconnectCalls).isZero()
    }

    private class StubHttpURLConnection(
        url: URL,
        private val responseCodeValue: Int,
        private val bodyValue: String,
        private val headerFieldsValue: Map<String, List<String>?>,
    ) : HttpURLConnection(url) {
        val existingRequestProperties = linkedMapOf<String, String>()
        private val output = ByteArrayOutputStream()
        var outputStreamRequested: Boolean = false
        var fixedLengthStreamingModeValue: Int = -1
        var nullErrorStream: Boolean = false
        var disconnectCalls: Int = 0

        override fun disconnect() {
            disconnectCalls += 1
        }

        override fun usingProxy(): Boolean = false

        override fun connect() {
        }

        override fun setRequestMethod(method: String) {
            this.method = method
        }

        override fun getResponseCode(): Int = responseCodeValue

        override fun getInputStream(): InputStream? {
            if (responseCodeValue >= 400) {
                return ByteArrayInputStream(ByteArray(0))
            }
            return ByteArrayInputStream(bodyValue.toByteArray(StandardCharsets.UTF_8))
        }

        override fun getErrorStream(): InputStream? {
            if (nullErrorStream) {
                return null
            }
            if (responseCodeValue >= 400) {
                return ByteArrayInputStream(bodyValue.toByteArray(StandardCharsets.UTF_8))
            }
            return null
        }

        override fun getOutputStream(): OutputStream {
            outputStreamRequested = true
            return output
        }

        override fun setFixedLengthStreamingMode(contentLength: Int) {
            fixedLengthStreamingModeValue = contentLength
        }

        override fun getHeaderFields(): Map<String, List<String>?> = headerFieldsValue

        override fun addRequestProperty(key: String, value: String) {
            super.addRequestProperty(key, value)
        }

        override fun getRequestProperty(key: String): String? = existingRequestProperties[key] ?: super.getRequestProperty(key)

        override fun setRequestProperty(key: String, value: String) {
            existingRequestProperties[key] = value
        }

        fun writtenBody(): String = output.toString(StandardCharsets.UTF_8.name())

        fun requestPropertyValues(name: String): List<String> = requestProperties[name] ?: emptyList()
    }
}
