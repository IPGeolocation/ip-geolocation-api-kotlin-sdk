package io.ipgeolocation.sdk.internal

import com.fasterxml.jackson.core.io.JsonStringEncoder
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder

internal class ErrorSanitizer(apiKey: String?) {
    private val variants = if (apiKey == null) emptyList() else listOf(
        apiKey,
        URLEncoder.encode(apiKey, "UTF-8"),
        String(JsonStringEncoder.getInstance().quoteAsString(apiKey)),
    ).distinct().sortedByDescending { it.length }

    fun message(value: String?): String? = variants.fold(value) { text, key -> text?.replace(key, "[REDACTED]") }

    fun cause(error: Throwable): Throwable {
        if (variants.isEmpty()) return error
        return IOException(error.javaClass.name).also { it.stackTrace = error.stackTrace }
    }

    fun containsKey(error: Throwable): Boolean {
        if (variants.isEmpty()) return false
        val rendered = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        return message(rendered) != rendered
    }
}
