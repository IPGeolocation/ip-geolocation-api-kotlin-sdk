package io.ipgeolocation.sdk.internal

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal object ObjectMappers {
    private fun createBaseMapper(): ObjectMapper {
        return jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    }

    fun default(): ObjectMapper = createBaseMapper()

    fun compact(): ObjectMapper = mapperWithInclusion(JsonInclude.Include.NON_NULL)

    fun full(): ObjectMapper = mapperWithInclusion(JsonInclude.Include.ALWAYS)

    private fun mapperWithInclusion(inclusion: JsonInclude.Include): ObjectMapper {
        return createBaseMapper().setDefaultPropertyInclusion(JsonInclude.Value.construct(inclusion, inclusion))
    }
}
