package ru.iuturakulov.mybudgetbackend

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ru.iuturakulov.mybudgetbackend.database.configureDatabase
import ru.iuturakulov.mybudgetbackend.plugins.configureAuth
import ru.iuturakulov.mybudgetbackend.plugins.configureBasic
import ru.iuturakulov.mybudgetbackend.plugins.configureDI
import ru.iuturakulov.mybudgetbackend.plugins.configureRequestValidation
import ru.iuturakulov.mybudgetbackend.plugins.configureRoutes
import ru.iuturakulov.mybudgetbackend.plugins.configureStatusPage
import ru.iuturakulov.mybudgetbackend.plugins.configureSwagger

fun main() {
    val config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    val port = config.property("ktor.deployment.port").getString().toInt()
    val host = config.property("ktor.deployment.host").getString()

    embeddedServer(Netty, port = port, host = host) {
        configureDatabase()
        configureBasic()
        configureDI()
        configureRequestValidation()
        configureAuth()
        configureSwagger()
        configureStatusPage()
        configureRoutes()
    }.start(wait = true)
}
