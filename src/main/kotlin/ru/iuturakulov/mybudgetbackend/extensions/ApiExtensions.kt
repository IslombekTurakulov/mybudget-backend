package ru.iuturakulov.mybudgetbackend.extensions

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiExtensions {

    suspend fun <T> callRequest(block: () -> T): T = withContext(Dispatchers.IO) {
        block()
    }

    suspend fun ApplicationCall.requiredParameters(vararg requiredParams: String): List<String>? {
        val missingParams = requiredParams.filterNot { this.parameters.contains(it) }
        if (missingParams.isNotEmpty()) {
            this.respond(ApiResponseState.success("Missing parameters: $missingParams", HttpStatusCode.BadRequest))
            return null
        }
        return requiredParams.map { this.parameters[it]!! }
    }

    fun generateVerificationCode(length: Int = 6): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    fun generatePassword(length: Int = 8): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}