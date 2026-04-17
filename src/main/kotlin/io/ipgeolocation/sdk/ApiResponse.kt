package io.ipgeolocation.sdk

data class ApiResponse<T>(
    val data: T,
    val metadata: ApiResponseMetadata,
)

