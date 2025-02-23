package ru.iuturakulov.mybudgetbackend.extensions

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val isSuccess: Boolean,
    val statusCode: @Contextual HttpStatusCode? = null,
    val data: T? = null,
    val error: T? = null
)


object ApiResponseState {
    fun <T> success(data: T, statsCode: HttpStatusCode?): ApiResponse<T> {
        return ApiResponse(
            isSuccess = true,
            data = data,
            statusCode = statsCode
        )
    }

    fun <T> failure(error: T, statsCode: HttpStatusCode?): ApiResponse<T> {
        return ApiResponse(
            isSuccess = false,
            error = error,
            statusCode = statsCode
        )
    }
}