package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.serialization.gson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import ru.iuturakulov.mybudgetbackend.plugins.LoggingPlugin.configureLogging

fun Application.configureBasic() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            serializeNulls()
        }
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    configureLogging()
}

