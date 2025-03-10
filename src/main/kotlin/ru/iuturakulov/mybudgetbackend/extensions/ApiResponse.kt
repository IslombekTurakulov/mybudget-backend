package ru.iuturakulov.mybudgetbackend.extensions

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val isSuccess: Boolean,  // Успех или нет
    val statusCode: Int,  // Числовой HTTP-код
    val data: T? = null,  // Данные (если успех)
    val error: T? = null  // Ошибка (если не успех)
)

object ApiResponseState {
    fun <T> success(data: T, statusCode: HttpStatusCode): ApiResponse<T> {
        return ApiResponse(
            isSuccess = true,
            data = data,
            statusCode = statusCode.value
        )
    }

    fun <T> failure(error: T, statusCode: HttpStatusCode): ApiResponse<T> {
        return ApiResponse(
            isSuccess = false,
            error = error,
            statusCode = statusCode.value
        )
    }
}