package io.ipgeolocation.sdk

import io.ipgeolocation.sdk.exceptions.SerializationException
import io.ipgeolocation.sdk.internal.ObjectMappers

object JsonOutput {
    @JvmStatic
    fun toJson(value: Any?): String = toJson(value, JsonOutputMode.COMPACT)

    @JvmStatic
    fun toJson(value: Any?, mode: JsonOutputMode): String = write(value, mode, pretty = false)

    @JvmStatic
    fun toPrettyJson(value: Any?): String = toPrettyJson(value, JsonOutputMode.COMPACT)

    @JvmStatic
    fun toPrettyJson(value: Any?, mode: JsonOutputMode): String = write(value, mode, pretty = true)

    private fun write(value: Any?, mode: JsonOutputMode, pretty: Boolean): String {
        val mapper = when (mode) {
            JsonOutputMode.COMPACT -> ObjectMappers.compact()
            JsonOutputMode.FULL -> ObjectMappers.full()
        }
        return try {
            if (pretty) {
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
            } else {
                mapper.writeValueAsString(value)
            }
        } catch (error: Exception) {
            throw SerializationException("Failed to serialize output as JSON", error)
        }
    }
}
