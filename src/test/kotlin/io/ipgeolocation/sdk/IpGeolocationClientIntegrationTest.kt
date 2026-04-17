package io.ipgeolocation.sdk

import com.sun.net.httpserver.HttpServer
import io.ipgeolocation.sdk.exceptions.UnauthorizedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.time.Duration

class IpGeolocationClientIntegrationTest {
    @Test
    fun performsSingleLookupAgainstLocalHttpServer() {
        val server = createServerOrSkip() ?: return
        server.createContext("/v3/ipgeo") { exchange ->
            val body = """{"ip":"8.8.8.8","location":{"country_name":"United States"}}""".toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.responseHeaders.add("X-Credits-Charged", "1")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        try {
            newClient(server).use { client ->
                val response = client.lookupIpGeolocation()

                assertThat(response.data.ip).isEqualTo("8.8.8.8")
                assertThat(response.data.location?.countryName).isEqualTo("United States")
                assertThat(response.metadata.creditsCharged).isEqualTo(1)
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun performsBulkLookupAgainstLocalHttpServer() {
        val server = createServerOrSkip() ?: return
        server.createContext("/v3/ipgeo-bulk") { exchange ->
            val requestBody = readBody(exchange.requestBody)
            val query = exchange.requestURI.query ?: ""
            if (!query.contains("apiKey=local-key") || !requestBody.contains("\"ips\"")) {
                exchange.sendResponseHeaders(400, -1)
                exchange.close()
                return@createContext
            }

            val body = """
                [
                  {"ip":"8.8.8.8"},
                  {"message":"invalid"}
                ]
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.responseHeaders.add("X-Credits-Charged", "2")
            exchange.responseHeaders.add("X-Successful-Records", "1")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        try {
            newClient(server).use { client ->
                val response = client.bulkLookupIpGeolocation(
                    BulkLookupIpGeolocationRequest(ips = listOf("8.8.8.8", "invalid")),
                )

                assertThat(response.data).hasSize(2)
                assertThat(response.data[0].isSuccess()).isTrue()
                assertThat(response.data[1].error?.message).isEqualTo("invalid")
                assertThat(response.metadata.creditsCharged).isEqualTo(2)
                assertThat(response.metadata.successfulRecords).isEqualTo(1)
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun mapsNon2xxFromLocalServerAndSupportsRawXml() {
        val server = createServerOrSkip() ?: return
        server.createContext("/v3/ipgeo") { exchange ->
            val query = exchange.requestURI.query ?: ""
            if (query.contains("output=xml")) {
                val body = "<ipgeo><ip>8.8.8.8</ip></ipgeo>".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/xml")
                exchange.responseHeaders.add("X-Credits-Charged", "1")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
                return@createContext
            }

            val body = """{"message":"invalid key"}""".toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(401, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        try {
            newClient(server).use { client ->
                assertThatThrownBy {
                    client.lookupIpGeolocation(LookupIpGeolocationRequest(ip = "8.8.8.8"))
                }
                    .isInstanceOfSatisfying(UnauthorizedException::class.java) { error ->
                        assertThat(error.statusCode).isEqualTo(401)
                        assertThat(error.apiMessage).isEqualTo("invalid key")
                    }

                val raw = client.lookupIpGeolocationRaw(
                    LookupIpGeolocationRequest(ip = "8.8.8.8", output = ResponseFormat.XML),
                )

                assertThat(raw.data).contains("<ipgeo>", "<ip>8.8.8.8</ip>")
                assertThat(raw.metadata.creditsCharged).isEqualTo(1)
            }
        } finally {
            server.stop(0)
        }
    }

    private fun createServerOrSkip(): HttpServer? {
        return try {
            HttpServer.create(InetSocketAddress(0), 0)
        } catch (_: SocketException) {
            Assumptions.assumeTrue(false, "Local TCP bind is unavailable")
            null
        } catch (_: IOException) {
            Assumptions.assumeTrue(false, "Local TCP bind is unavailable")
            null
        }
    }

    private fun newClient(server: HttpServer): IpGeolocationClient {
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        return IpGeolocationClient(
            IpGeolocationClientConfig(
                apiKey = "local-key",
                baseUrl = baseUrl,
                connectTimeout = Duration.ofSeconds(2),
                readTimeout = Duration.ofSeconds(5),
            ),
        )
    }

    private fun readBody(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) {
                break
            }
            output.write(buffer, 0, read)
        }
        return String(output.toByteArray(), StandardCharsets.UTF_8)
    }
}
