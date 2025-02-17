package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import org.slf4j.event.Level
import ru.iuturakulov.mybudgetbackend.plugins.LoggingPlugin.configureLogging

fun Application.configureBasic() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            // serializeNulls()
        }
    }
    configureLogging()
}

