package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.server.application.*
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import ru.iuturakulov.mybudgetbackend.di.appModule
import ru.iuturakulov.mybudgetbackend.di.controllerModule

fun Application.configureDI() {
    install(Koin) {
        slf4jLogger(Level.INFO)
        modules(appModule)
    }
}
