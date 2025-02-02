package ru.iuturakulov.mybudgetbackend.extensions

import io.ktor.http.*

data class ApiResponse(
    val isSuccess: Boolean,
    val statusCode: HttpStatusCode? = null,
    val data: Any? = null,
    val error: Any? = null
)


object ApiResponseState {
    fun <T> success(data: T, statsCode: HttpStatusCode?): ApiResponse {
        return ApiResponse(
            isSuccess = true,
            data = data,
            statusCode = statsCode
        )
    }

    fun <T> failure(error: T, statsCode: HttpStatusCode?): ApiResponse {
        return ApiResponse(
            isSuccess = false,
            error = error,
            statusCode = statsCode
        )
    }
}