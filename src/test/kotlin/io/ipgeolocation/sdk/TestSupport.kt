package io.ipgeolocation.sdk

internal fun headers(vararg keyValues: String): Map<String, List<String>> {
    require(keyValues.size % 2 == 0) { "headers requires an even number of key/value arguments" }
    if (keyValues.isEmpty()) {
        return emptyMap()
    }

    val result = linkedMapOf<String, List<String>>()
    keyValues.toList().chunked(2).forEach { (name, value) ->
        result[name] = listOf(value)
    }
    return result
}
