package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
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

