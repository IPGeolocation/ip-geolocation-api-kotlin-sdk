package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.ApiException
import io.ipgeolocation.sdk.exceptions.RequestTimeoutException
import io.ipgeolocation.sdk.exceptions.SerializationException
import io.ipgeolocation.sdk.exceptions.TransportException
import io.ipgeolocation.sdk.internal.ObjectMappers
import io.ipgeolocation.sdk.internal.ResponseMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.time.Duration

class SecurityRegressionTest {
    private val key = "example-secret /+"
    private val encodedKey = URLEncoder.encode(key, "UTF-8")

    @Test
    fun apiErrorsHideKeysAcrossAllLookupMethodsAndBodyFormats() {
        val bodies = listOf(
            """{"message":"Rejected $key"}""",
            """{"error":{"message":"Rejected $encodedKey"}}""",
            """{"detail":{"message":"Rejected $key"}}""",
            """"Rejected $key"""",
            "x".repeat(500) + key,
            "x".repeat(500) + encodedKey,
        )
        for (body in bodies) {
            for (method in methods) {
                val executor = TestHttpExecutor().apply { enqueueResponse(401, body) }
                val failure = catchThrowable { method(client(executor)) }
                assertThat(failure).isInstanceOf(io.ipgeolocation.sdk.exceptions.UnauthorizedException::class.java)
                val apiError = failure as ApiException
                assertThat(apiError.statusCode).isEqualTo(401)
                assertThat(apiError.message).contains("[REDACTED]")
                assertThat(apiError.apiMessage).doesNotContain("example-secret")
                assertHidden(apiError)
                assertThat(executor.capturedRequests.single().url.rawQuery).contains("apiKey=$encodedKey")
            }
        }
    }

    @Test
    fun transportErrorsDoNotRetainOriginalCausesOrSuppressedErrors() {
        for (failure in listOf(IOException("GET ?apiKey=$encodedKey"), SocketTimeoutException("timeout $key"))) {
            failure.initCause(IllegalStateException("nested $key"))
            failure.addSuppressed(IOException("suppressed $encodedKey"))
            val executor = TestHttpExecutor().apply { enqueueThrowable(failure) }
            val result = catchThrowable { client(executor).lookupIpGeolocation() }
            assertThat(result).isInstanceOf(TransportException::class.java)
            if (failure is SocketTimeoutException) assertThat(result).isInstanceOf(RequestTimeoutException::class.java)
            assertThat(result.cause).isNotSameAs(failure)
            assertThat(result.cause?.cause).isNull()
            assertThat(result.cause?.suppressed).isEmpty()
            assertHidden(result)
        }
    }

    @Test
    fun requestOriginOnlyRetainsDiagnosticCause() {
        val cause = IOException("connection reset")
        val executor = TestHttpExecutor().apply { enqueueThrowable(cause) }
        val config = IpGeolocationClientConfig(requestOrigin = "https://example.com")
        val failure = catchThrowable { IpGeolocationClient(config, executor, ObjectMappers.default()).lookupIpGeolocation() }
        assertThat(failure).isInstanceOf(TransportException::class.java)
        assertThat(failure.cause).isSameAs(cause)
    }

    @Test
    fun unexpectedErrorsCannotExposeKeysThroughTheirStackTrace() {
        val cause = IllegalStateException("unexpected failure").apply { addSuppressed(IOException(key)) }
        val executor = TestHttpExecutor().apply { enqueueThrowable(cause) }
        assertHidden(catchThrowable { client(executor).lookupIpGeolocation() })
    }

    @Test
    fun errorsWithoutKeysKeepTheirOriginalTypeAndDetails() {
        for (config in listOf(IpGeolocationClientConfig(apiKey = key), IpGeolocationClientConfig(requestOrigin = "https://example.com"))) {
            val original = IllegalStateException("unexpected failure")
            val executor = TestHttpExecutor().apply { enqueueThrowable(original) }
            assertThat(catchThrowable { IpGeolocationClient(config, executor, ObjectMappers.default()).lookupIpGeolocation() })
                .isSameAs(original)
        }
    }

    @Test
    fun transportApiExceptionsRetainTheirStatusAndTypeWithoutExposingKeys() {
        for (status in listOf(400, 401, 404, 405, 413, 415, 418, 423, 429, 499, 503)) {
            val original = ResponseMapper(ObjectMappers.default()).toApiException(status, "Rejected $key")
            val executor = TestHttpExecutor().apply { enqueueThrowable(original) }
            val failure = catchThrowable { client(executor).lookupIpGeolocation() } as ApiException
            assertThat(failure.javaClass).isEqualTo(original.javaClass)
            assertThat(failure.statusCode).isEqualTo(status)
            assertThat(failure.apiMessage).isEqualTo("Rejected [REDACTED]")
            assertHidden(failure)
        }
        for (original in listOf(ApiException("clean", 418, null), ApiException("clean", 418, key), ApiException("clean", 418, null).apply { addSuppressed(IOException(key)) })) {
            val executor = TestHttpExecutor().apply { enqueueThrowable(original) }
            val failure = catchThrowable { client(executor).lookupIpGeolocation() } as ApiException
            assertThat(failure.statusCode).isEqualTo(418)
            assertHidden(failure)
            if (original.apiMessage == null && original.suppressed.isEmpty()) assertThat(failure).isSameAs(original)
        }
    }

    @Test
    fun malformedModelFieldsCannotExposeKeysInParserErrors() {
        val body = """{"location":{"is_eu":"$key"}}"""
        for (bulk in listOf(false, true)) {
            val executor = TestHttpExecutor().apply { enqueueResponse(200, if (bulk) "[$body]" else body) }
            val client = client(executor)
            val failure = catchThrowable {
                if (bulk) client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(listOf("8.8.8.8")))
                else client.lookupIpGeolocation()
            }
            assertThat(failure).isInstanceOf(SerializationException::class.java)
            assertHidden(failure)
        }
    }

    @Test
    fun jsonEscapedKeysAreHiddenBeforeErrorBodyTruncation() {
        val specialKey = "example-\"quoted\"\\key"
        val escaped = ObjectMappers.default().writeValueAsString(specialKey).removeSurrounding("\"")
        val executor = TestHttpExecutor().apply { enqueueResponse(502, "invalid " + escaped) }
        val client = IpGeolocationClient(IpGeolocationClientConfig(apiKey = specialKey), executor, ObjectMappers.default())
        val failure = catchThrowable { client.lookupIpGeolocation() } as ApiException
        assertThat(failure.apiMessage).isEqualTo("invalid [REDACTED]")
    }

    @Test
    fun successfulDataAndRawBodiesAreUnchangedWhileBulkErrorsHideKeys() {
        val single = """{"ip":"$key","unknown":"retained upstream"}"""
        val bulk = """[{"ip":"$key"},{"error":{"message":"Rejected $key"}}]"""
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, single, mapOf("X-Test" to listOf(key)))
            enqueueResponse(200, single)
            enqueueResponse(200, bulk)
            enqueueResponse(200, bulk)
        }
        val client = client(executor)
        val response = client.lookupIpGeolocation()
        assertThat(response.data.ip).isEqualTo(key)
        assertThat(response.metadata.firstHeaderValue("X-Test")).isEqualTo(key)
        assertThat(client.lookupIpGeolocationRaw().data).isEqualTo(single)
        val results = client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(listOf("8.8.8.8"))).data
        assertThat(results[0].data?.ip).isEqualTo(key)
        assertThat(results[1].error?.message).isEqualTo("Rejected [REDACTED]")
        assertThat(client.bulkLookupIpGeolocationRaw(BulkLookupIpGeolocationRequest(listOf("8.8.8.8"))).data).isEqualTo(bulk)
    }

    @Test
    fun typedLookupsRejectMissingOrNonObjectData() {
        for (body in listOf("", " ", "null", "[]", "[{}]", "true", "123", "\"text\"", "{broken", "{} {}", "{}garbage")) {
            val executor = TestHttpExecutor().apply { enqueueResponse(200, body) }
            assertThatThrownBy { client(executor).lookupIpGeolocation() }.isInstanceOf(SerializationException::class.java)
        }
        for (body in listOf("", " ", "null", "{}", "[null]", "[true]", "[1]", "[\"text\"]", "[[]]", "[] []", "[]garbage")) {
            val executor = TestHttpExecutor().apply { enqueueResponse(200, body) }
            assertThatThrownBy {
                client(executor).bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(listOf("8.8.8.8")))
            }.isInstanceOf(SerializationException::class.java)
        }
    }

    @Test
    fun projectedObjectsAndEmptyBulkArraysRemainValid() {
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, "{}")
            enqueueResponse(200, "[]")
            enqueueResponse(200, "[{}]")
        }
        val client = client(executor)
        assertThat(client.lookupIpGeolocation().data.ip).isNull()
        assertThat(client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(listOf("8.8.8.8"))).data).isEmpty()
        assertThat(client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(listOf("8.8.8.8"))).data.single().isSuccess()).isTrue()
    }

    @Test
    fun arabicIsSentInSingleAndBulkRequests() {
        val arabic = Language.fromCode("AR")
        assertThat(arabic).isNotNull()
        assertThat(arabic?.code).isEqualTo("ar")
        assertThat(Language.entries.take(12).map { it.code }).containsExactly("en", "de", "ru", "ja", "fr", "cn", "es", "cs", "it", "ko", "fa", "pt")
        val executor = TestHttpExecutor().apply {
            enqueueResponse(200, "{}")
            enqueueResponse(200, "[{}]")
        }
        val client = client(executor)
        client.lookupIpGeolocation(LookupIpGeolocationRequest(lang = arabic))
        client.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(listOf("8.8.8.8"), lang = arabic))
        assertThat(executor.capturedRequests.map { it.url.rawQuery }).allSatisfy { assertThat(it).contains("lang=ar") }
    }

    @Test
    fun veryLargeTimeoutsRemainUsableInClientsAndDiagnosticOutput() {
        val duration = Duration.ofSeconds(Long.MAX_VALUE)
        val config = IpGeolocationClientConfig(apiKey = key, connectTimeout = duration, readTimeout = duration)
        IpGeolocationClient(config).close()
        assertThat(config.connectTimeout).isEqualTo(duration)
        assertThat(config.readTimeout).isEqualTo(duration)
        assertThat(config.safeMap()["connectTimeoutMs"]).isEqualTo(Long.MAX_VALUE)
        assertThat(config.safeMap()["readTimeoutMs"]).isEqualTo(Long.MAX_VALUE)
        assertThat(config.toString()).doesNotContain(key)
        val executor = TestHttpExecutor().apply { enqueueThrowable(SocketTimeoutException("timeout $key")) }
        val failure = catchThrowable { IpGeolocationClient(config, executor, ObjectMappers.default()).lookupIpGeolocation() }
        assertThat(failure).isInstanceOf(RequestTimeoutException::class.java)
        assertHidden(failure)
    }

    private fun client(executor: TestHttpExecutor) = IpGeolocationClient(IpGeolocationClientConfig(apiKey = key), executor, ObjectMappers.default())

    private fun assertHidden(error: Throwable) {
        val rendered = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        assertThat(rendered).doesNotContain(key, encodedKey)
    }

    private val methods: List<(IpGeolocationClient) -> Any> = listOf(
        { it.lookupIpGeolocation() },
        { it.lookupIpGeolocationRaw() },
        { it.bulkLookupIpGeolocation(BulkLookupIpGeolocationRequest(listOf("8.8.8.8"))) },
        { it.bulkLookupIpGeolocationRaw(BulkLookupIpGeolocationRequest(listOf("8.8.8.8"))) },
    )
}
