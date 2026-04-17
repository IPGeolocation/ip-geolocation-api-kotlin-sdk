package io.ipgeolocation.sdk.exceptions

open class IpGeolocationException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)
}

open class ApiException(
    message: String,
    val statusCode: Int,
    val apiMessage: String?,
) : IpGeolocationException(message)

class BadRequestException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class UnauthorizedException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class NotFoundException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class MethodNotAllowedException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class PayloadTooLargeException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class UnsupportedMediaTypeException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class LockedException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class RateLimitException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class ClientClosedRequestException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

class ServerErrorException(
    message: String,
    statusCode: Int,
    apiMessage: String?,
) : ApiException(message, statusCode, apiMessage)

open class TransportException(
    message: String,
    cause: Throwable?,
) : IpGeolocationException(message, cause)

class RequestTimeoutException(
    message: String,
    cause: Throwable?,
) : TransportException(message, cause)

class SerializationException(
    message: String,
    cause: Throwable? = null,
) : IpGeolocationException(message, cause)

class ValidationException(
    message: String,
) : IpGeolocationException(message)
