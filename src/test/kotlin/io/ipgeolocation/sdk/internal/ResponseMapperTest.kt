package io.ipgeolocation.sdk.internal

import io.ipgeolocation.sdk.exceptions.ApiException
import io.ipgeolocation.sdk.exceptions.ClientClosedRequestException
import io.ipgeolocation.sdk.exceptions.LockedException
import io.ipgeolocation.sdk.exceptions.MethodNotAllowedException
import io.ipgeolocation.sdk.exceptions.NotFoundException
import io.ipgeolocation.sdk.exceptions.PayloadTooLargeException
import io.ipgeolocation.sdk.exceptions.RateLimitException
import io.ipgeolocation.sdk.exceptions.SerializationException
import io.ipgeolocation.sdk.exceptions.ServerErrorException
import io.ipgeolocation.sdk.exceptions.UnsupportedMediaTypeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ResponseMapperTest {
    private val mapper = ResponseMapper(ObjectMappers.default())

    @Test
    fun apiExceptionParsingHandlesBlankTextAndNestedMessages() {
        val blank = mapper.toApiException(418, "")
        val nested = mapper.toApiException(502, """{"detail":{"message":"upstream"}}""")
        val textual = mapper.toApiException(400, "plain text body")
        val jsonString = mapper.toApiException(400, """"plain text json string"""")
        val errorString = mapper.toApiException(400, """{"error":"direct error"}""")
        val detailString = mapper.toApiException(400, """{"detail":"detail string"}""")

        assertThat(blank).isInstanceOf(ApiException::class.java)
        assertThat(blank.apiMessage).isNull()
        assertThat(blank.message).isEqualTo("API request failed with HTTP status 418")
        assertThat(nested).isInstanceOf(ServerErrorException::class.java)
        assertThat(nested.apiMessage).isEqualTo("upstream")
        assertThat(textual.apiMessage).isEqualTo("plain text body")
        assertThat(jsonString.apiMessage).isEqualTo("plain text json string")
        assertThat(errorString.apiMessage).isEqualTo("direct error")
        assertThat(detailString.apiMessage).isEqualTo("detail string")
    }

    @Test
    fun metadataParsingHandlesMissingInvalidAndBothSuccessfulRecordHeaderSpellings() {
        val metadata = mapper.toMetadata(
            statusCode = 200,
            durationMs = 15,
            rawHeaders = linkedMapOf(
                "X-Credits-Charged" to listOf("bad"),
                "x-successful-records" to listOf("7"),
                "X-Trace-Id" to emptyList(),
            ),
        )
        val singularFallback = mapper.toMetadata(
            statusCode = 200,
            durationMs = 18,
            rawHeaders = linkedMapOf(
                "x-successful-record" to listOf("3"),
            ),
        )

        assertThat(metadata.creditsCharged).isNull()
        assertThat(metadata.successfulRecords).isEqualTo(7)
        assertThat(metadata.rawHeaders).containsKeys("X-Credits-Charged", "x-successful-records", "X-Trace-Id")
        assertThat(singularFallback.successfulRecords).isEqualTo(3)
    }

    @Test
    fun statusSpecificApiExceptionsMapToTheExpectedSubclass() {
        assertThat(mapper.toApiException(404, """{"message":"missing"}""")).isInstanceOf(NotFoundException::class.java)
        assertThat(mapper.toApiException(405, """{"message":"method"}""")).isInstanceOf(MethodNotAllowedException::class.java)
        assertThat(mapper.toApiException(413, """{"message":"too large"}""")).isInstanceOf(PayloadTooLargeException::class.java)
        assertThat(mapper.toApiException(415, """{"message":"unsupported"}""")).isInstanceOf(UnsupportedMediaTypeException::class.java)
        assertThat(mapper.toApiException(423, """{"message":"locked"}""")).isInstanceOf(LockedException::class.java)
        assertThat(mapper.toApiException(429, """{"message":"limited"}""")).isInstanceOf(RateLimitException::class.java)
        assertThat(mapper.toApiException(499, """{"message":"closed"}""")).isInstanceOf(ClientClosedRequestException::class.java)
        assertThat(mapper.toApiException(502, """{"message":"upstream"}""")).isInstanceOf(ServerErrorException::class.java)
    }

    @Test
    fun readBodyAndBulkParsingHandleSuccessAndErrorShapes() {
        val parsed = mapper.readBody("""{"ip":"8.8.8.8"}""", io.ipgeolocation.sdk.model.IpGeolocationResponse::class.java)
        val bulk = mapper.parseBulkResponse(
            """
                [
                  {"ip":"8.8.8.8"},
                  {"message":"top level"},
                  {"error":"direct error"},
                  {"detail":"detail string"},
                  {"detail":{"message":"nested error"}}
                ]
            """.trimIndent(),
        )

        assertThat(parsed.ip).isEqualTo("8.8.8.8")
        assertThat(bulk[0].data?.ip).isEqualTo("8.8.8.8")
        assertThat(bulk[1].error?.message).isEqualTo("top level")
        assertThat(bulk[2].error?.message).isEqualTo("direct error")
        assertThat(bulk[3].error?.message).isEqualTo("detail string")
        assertThat(bulk[4].error?.message).isEqualTo("nested error")
    }

    @Test
    fun rejectsMalformedBodiesAndNonArrayBulkPayloads() {
        assertThatThrownBy {
            mapper.readBody("{bad", io.ipgeolocation.sdk.model.IpGeolocationResponse::class.java)
        }
            .isInstanceOf(SerializationException::class.java)
            .hasMessageContaining("deserialize API response")

        assertThatThrownBy { mapper.parseBulkResponse("{\"ip\":\"8.8.8.8\"}") }
            .isInstanceOf(SerializationException::class.java)
            .hasMessageContaining("expected an array payload")

        val longBody = buildString {
            repeat(700) { append('x') }
        }
        assertThat(mapper.toApiException(400, longBody).apiMessage).hasSize(512)
    }
}
