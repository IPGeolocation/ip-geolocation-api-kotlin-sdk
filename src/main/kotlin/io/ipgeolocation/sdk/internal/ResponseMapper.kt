package io.ipgeolocation.sdk.internal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ipgeolocation.sdk.ApiResponseMetadata
import io.ipgeolocation.sdk.exceptions.ApiException
import io.ipgeolocation.sdk.exceptions.BadRequestException
import io.ipgeolocation.sdk.exceptions.ClientClosedRequestException
import io.ipgeolocation.sdk.exceptions.LockedException
import io.ipgeolocation.sdk.exceptions.MethodNotAllowedException
import io.ipgeolocation.sdk.exceptions.NotFoundException
import io.ipgeolocation.sdk.exceptions.PayloadTooLargeException
import io.ipgeolocation.sdk.exceptions.RateLimitException
import io.ipgeolocation.sdk.exceptions.SerializationException
import io.ipgeolocation.sdk.exceptions.ServerErrorException
import io.ipgeolocation.sdk.exceptions.UnauthorizedException
import io.ipgeolocation.sdk.exceptions.UnsupportedMediaTypeException
import io.ipgeolocation.sdk.model.BulkLookupError
import io.ipgeolocation.sdk.model.BulkLookupResult
import io.ipgeolocation.sdk.model.IpGeolocationResponse
import java.io.IOException

internal class ResponseMapper(
    private val objectMapper: ObjectMapper,
    apiKey: String? = null,
) {
    private val errors = ErrorSanitizer(apiKey)

    fun <T> readBody(body: String, type: Class<T>): T {
        val root = parseJson(body, "Failed to deserialize API response")
        if (!root.isObject) {
            throw SerializationException("Failed to deserialize API response: expected an object payload")
        }
        try {
            return objectMapper.treeToValue(root, type) ?: throw IOException("Expected a JSON object")
        } catch (error: Exception) {
            throw SerializationException("Failed to deserialize API response", errors.cause(error))
        }
    }

    fun parseBulkResponse(body: String): List<BulkLookupResult> {
        val root = parseJson(body, "Failed to deserialize bulk lookup response")
        if (!root.isArray) {
            throw SerializationException("Failed to deserialize bulk response: expected an array payload")
        }

        return root.map { node ->
            if (!node.isObject) {
                throw SerializationException("Failed to deserialize bulk response: expected an object for each result")
            }
            val errorMessage = extractBulkErrorMessage(node)
            if (!errorMessage.isNullOrBlank()) {
                BulkLookupResult(error = BulkLookupError(errors.message(errorMessage)!!))
            } else {
                try {
                    BulkLookupResult(data = objectMapper.treeToValue(node, IpGeolocationResponse::class.java))
                } catch (error: Exception) {
                    throw SerializationException("Failed to deserialize API response", errors.cause(error))
                }
            }
        }
    }

    fun toApiException(statusCode: Int, body: String): ApiException {
        val apiMessage = extractApiMessage(body)
        val message = if (apiMessage == null) {
            "API request failed with HTTP status $statusCode"
        } else {
            "API request failed with HTTP status $statusCode: $apiMessage"
        }

        return apiException(statusCode, message, apiMessage)
    }

    fun sanitizeApiException(error: ApiException): ApiException {
        val message = errors.message(error.message)
        val apiMessage = errors.message(error.apiMessage)
        if (message == error.message && apiMessage == error.apiMessage && !errors.containsKey(error)) return error
        return apiException(error.statusCode, message.orEmpty(), apiMessage)
    }

    private fun apiException(statusCode: Int, message: String, apiMessage: String?): ApiException {
        return when (statusCode) {
            400 -> BadRequestException(message, statusCode, apiMessage)
            401 -> UnauthorizedException(message, statusCode, apiMessage)
            404 -> NotFoundException(message, statusCode, apiMessage)
            405 -> MethodNotAllowedException(message, statusCode, apiMessage)
            413 -> PayloadTooLargeException(message, statusCode, apiMessage)
            415 -> UnsupportedMediaTypeException(message, statusCode, apiMessage)
            423 -> LockedException(message, statusCode, apiMessage)
            429 -> RateLimitException(message, statusCode, apiMessage)
            499 -> ClientClosedRequestException(message, statusCode, apiMessage)
            in 500..599 -> ServerErrorException(message, statusCode, apiMessage)
            else -> ApiException(message, statusCode, apiMessage)
        }
    }

    fun toMetadata(
        statusCode: Int,
        durationMs: Long,
        rawHeaders: Map<String, List<String>>,
    ): ApiResponseMetadata {
        return ApiResponseMetadata(
            creditsCharged = parseIntHeader(firstHeaderIgnoreCase(rawHeaders, "X-Credits-Charged")),
            successfulRecords = parseIntHeader(
                firstHeaderIgnoreCase(rawHeaders, "X-Successful-Record", "X-Successful-Records"),
            ),
            statusCode = statusCode,
            durationMs = durationMs,
            rawHeaders = rawHeaders,
        )
    }

    private fun parseJson(body: String, message: String): JsonNode {
        try {
            return objectMapper.readTree(body) ?: throw IOException("Expected a JSON value")
        } catch (error: Exception) {
            throw SerializationException(message, errors.cause(error))
        }
    }

    private fun extractBulkErrorMessage(node: JsonNode): String? {
        return directText(node.get("message"))
            ?: directText(node.get("error"))
            ?: directText(node.get("detail"))
            ?: directText(node.get("error")?.get("message"))
            ?: directText(node.get("detail")?.get("message"))
    }

    private fun extractApiMessage(body: String?): String? {
        if (body.isNullOrBlank()) {
            return null
        }
        val normalized = body.trim()
        return try {
            val root = objectMapper.readTree(normalized)
            val message = directText(root)
                ?: directText(root.get("message"))
                ?: directText(root.get("error"))
                ?: directText(root.get("detail"))
                ?: directText(root.get("error")?.get("message"))
                ?: directText(root.get("detail")?.get("message"))
            if (message == null) truncate(errors.message(normalized)!!) else errors.message(message)
        } catch (_: Exception) {
            truncate(errors.message(normalized)!!)
        }
    }

    private fun directText(node: JsonNode?): String? {
        if (node == null || !node.isTextual) {
            return null
        }
        return node.asText().trim().ifEmpty { null }
    }

    private fun truncate(value: String): String = if (value.length > 512) value.substring(0, 512) else value

    private fun parseIntHeader(value: String?): Int? {
        if (value.isNullOrBlank()) {
            return null
        }
        val normalized = value.trim()
        if (!normalized.all { it.isDigit() }) {
            return null
        }
        return normalized.toIntOrNull()
    }

    private fun firstHeaderIgnoreCase(headers: Map<String, List<String>>, vararg names: String): String? {
        for (name in names) {
            val value = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()
            if (value != null) {
                return value
            }
        }
        return null
    }
}
