package io.ipgeolocation.sdk

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.ipgeolocation.sdk.exceptions.ApiException
import io.ipgeolocation.sdk.exceptions.RequestTimeoutException
import io.ipgeolocation.sdk.exceptions.SerializationException
import io.ipgeolocation.sdk.exceptions.TransportException
import io.ipgeolocation.sdk.exceptions.ValidationException
import io.ipgeolocation.sdk.internal.HttpExecutor
import io.ipgeolocation.sdk.internal.HttpRequestData
import io.ipgeolocation.sdk.internal.HttpResponseData
import io.ipgeolocation.sdk.internal.JavaNetHttpExecutor
import io.ipgeolocation.sdk.internal.ObjectMappers
import io.ipgeolocation.sdk.internal.ResponseMapper
import io.ipgeolocation.sdk.internal.SdkVersion
import io.ipgeolocation.sdk.internal.UriBuilder
import io.ipgeolocation.sdk.internal.mergeHeaders
import io.ipgeolocation.sdk.internal.resolveUserAgentHeader
import io.ipgeolocation.sdk.model.BulkLookupResult
import io.ipgeolocation.sdk.model.IpGeolocationResponse
import java.io.Closeable
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Duration
import kotlin.system.measureTimeMillis

class IpGeolocationClient internal constructor(
    private val config: IpGeolocationClientConfig,
    private val executor: HttpExecutor,
    private val objectMapper: ObjectMapper,
) : Closeable {
    private val responseMapper: ResponseMapper = ResponseMapper(this.objectMapper)
    @Volatile
    private var closed: Boolean = false

    public constructor(
        config: IpGeolocationClientConfig,
    ) : this(
        config = config,
        executor = JavaNetHttpExecutor(config.connectTimeout, config.maxResponseBodyChars),
        objectMapper = ObjectMappers.default(),
    )

    @JvmOverloads
    fun lookupIpGeolocation(
        request: LookupIpGeolocationRequest? = LookupIpGeolocationRequest(),
    ): ApiResponse<IpGeolocationResponse> {
        ensureOpen()
        val typedRequest = request ?: throw ValidationException("request must not be null")
        ensureSingleLookupConfigured()
        val normalized = normalizeLookupRequest(typedRequest)
        validateJsonOutput(normalized.output)
        val execution = executeWithMetrics(buildLookupHttpRequest(normalized))
        if (execution.response.statusCode / 100 != 2) {
            throw responseMapper.toApiException(execution.response.statusCode, execution.response.body)
        }

        return ApiResponse(
            data = responseMapper.readBody(execution.response.body, IpGeolocationResponse::class.java),
            metadata = responseMapper.toMetadata(
                execution.response.statusCode,
                execution.durationMs,
                execution.response.headers,
            ),
        )
    }

    @JvmOverloads
    fun lookupIpGeolocationRaw(
        request: LookupIpGeolocationRequest? = LookupIpGeolocationRequest(),
    ): ApiResponse<String> {
        ensureOpen()
        val typedRequest = request ?: throw ValidationException("request must not be null")
        ensureSingleLookupConfigured()
        val normalized = normalizeLookupRequest(typedRequest)
        val execution = executeWithMetrics(buildLookupHttpRequest(normalized))
        if (execution.response.statusCode / 100 != 2) {
            throw responseMapper.toApiException(execution.response.statusCode, execution.response.body)
        }

        return ApiResponse(
            data = execution.response.body,
            metadata = responseMapper.toMetadata(
                execution.response.statusCode,
                execution.durationMs,
                execution.response.headers,
            ),
        )
    }

    fun bulkLookupIpGeolocation(
        request: BulkLookupIpGeolocationRequest?,
    ): ApiResponse<List<BulkLookupResult>> {
        ensureOpen()
        val typedRequest = request ?: throw ValidationException("request must not be null")
        ensureBulkLookupConfigured()
        val normalized = normalizeBulkLookupRequest(typedRequest)
        validateJsonOutput(normalized.output)
        val execution = executeWithMetrics(buildBulkHttpRequest(normalized))
        if (execution.response.statusCode / 100 != 2) {
            throw responseMapper.toApiException(execution.response.statusCode, execution.response.body)
        }

        return ApiResponse(
            data = responseMapper.parseBulkResponse(execution.response.body),
            metadata = responseMapper.toMetadata(
                execution.response.statusCode,
                execution.durationMs,
                execution.response.headers,
            ),
        )
    }

    fun bulkLookupIpGeolocationRaw(
        request: BulkLookupIpGeolocationRequest?,
    ): ApiResponse<String> {
        ensureOpen()
        val typedRequest = request ?: throw ValidationException("request must not be null")
        ensureBulkLookupConfigured()
        val normalized = normalizeBulkLookupRequest(typedRequest)
        val execution = executeWithMetrics(buildBulkHttpRequest(normalized))
        if (execution.response.statusCode / 100 != 2) {
            throw responseMapper.toApiException(execution.response.statusCode, execution.response.body)
        }

        return ApiResponse(
            data = execution.response.body,
            metadata = responseMapper.toMetadata(
                execution.response.statusCode,
                execution.durationMs,
                execution.response.headers,
            ),
        )
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        executor.close()
    }

    private fun buildLookupHttpRequest(
        request: NormalizedLookupRequest,
    ): HttpRequestData {
        val url = UriBuilder.build(
            baseUrl = config.baseUrl,
            path = "/v3/ipgeo",
            queryParams = linkedMapOf(
                "apiKey" to config.apiKey,
                "ip" to request.ip,
                "lang" to request.lang,
                "include" to request.include.joinToString(",").ifBlank { null },
                "fields" to request.fields.joinToString(",").ifBlank { null },
                "excludes" to request.excludes.joinToString(",").ifBlank { null },
                "output" to request.output.wireValue,
            ),
        )

        val headers = mergeHeaders(
            request.headers.mapValues { listOf(it.value) },
            if (config.requestOrigin == null) null else mapOf("Origin" to listOf(config.requestOrigin)),
            mapOf(
                "User-Agent" to listOf(resolveUserAgentHeader(request.userAgent, request.headers, defaultUserAgent())),
                "Accept" to listOf(if (request.output == ResponseFormat.XML) "application/xml" else "application/json"),
            ),
        )

        return HttpRequestData(
            url = url,
            method = "GET",
            headers = headers,
            body = null,
            timeout = config.readTimeout,
        )
    }

    private fun buildBulkHttpRequest(
        request: NormalizedBulkLookupRequest,
    ): HttpRequestData {
        val url = UriBuilder.build(
            baseUrl = config.baseUrl,
            path = "/v3/ipgeo-bulk",
            queryParams = linkedMapOf(
                "apiKey" to config.apiKey,
                "lang" to request.lang,
                "include" to request.include.joinToString(",").ifBlank { null },
                "fields" to request.fields.joinToString(",").ifBlank { null },
                "excludes" to request.excludes.joinToString(",").ifBlank { null },
                "output" to request.output.wireValue,
            ),
        )

        val payload = try {
            objectMapper.writeValueAsString(mapOf("ips" to request.ips))
        } catch (error: JsonProcessingException) {
            throw SerializationException("Failed to serialize bulk lookup request body", error)
        }

        val headers = mergeHeaders(
            request.headers.mapValues { listOf(it.value) },
            if (config.requestOrigin == null) null else mapOf("Origin" to listOf(config.requestOrigin)),
            mapOf(
                "User-Agent" to listOf(resolveUserAgentHeader(request.userAgent, request.headers, defaultUserAgent())),
                "Accept" to listOf(if (request.output == ResponseFormat.XML) "application/xml" else "application/json"),
                "Content-Type" to listOf("application/json"),
            ),
        )

        return HttpRequestData(
            url = url,
            method = "POST",
            headers = headers,
            body = payload,
            timeout = config.readTimeout,
        )
    }

    private fun executeWithMetrics(request: HttpRequestData): ExecutionResult {
        var response: HttpResponseData? = null
        val durationMs = try {
            measureTimeMillis {
                response = executor.send(request)
            }
        } catch (error: SocketTimeoutException) {
            throw RequestTimeoutException(
                "HTTP request timed out after ${request.timeout.toMillis()}ms while waiting for response data",
                error,
            )
        } catch (error: ApiException) {
            throw error
        } catch (error: IOException) {
            throw TransportException("HTTP transport error", error)
        } catch (error: RuntimeException) {
            throw error
        }

        return ExecutionResult(response = response!!, durationMs = durationMs)
    }

    private fun ensureOpen() {
        if (closed) {
            throw ValidationException("client is closed")
        }
    }

    private fun ensureSingleLookupConfigured() {
        if (config.apiKey == null && config.requestOrigin == null) {
            throw ValidationException("single lookup requires apiKey or requestOrigin in client config")
        }
    }

    private fun ensureBulkLookupConfigured() {
        if (config.apiKey == null) {
            throw ValidationException("bulk lookup requires apiKey in client config")
        }
    }

    private data class ExecutionResult(
        val response: HttpResponseData,
        val durationMs: Long,
    )

    companion object {
        @JvmStatic
        fun defaultUserAgent(): String = "ipgeolocation-kotlin-sdk/${SdkVersion.VERSION}"
    }
}

private fun validateJsonOutput(output: ResponseFormat) {
    if (output == ResponseFormat.XML) {
        throw ValidationException("XML output is not supported by typed methods. Use ResponseFormat.JSON.")
    }
}
