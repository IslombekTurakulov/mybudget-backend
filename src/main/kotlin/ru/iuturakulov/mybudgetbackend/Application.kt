package ru.iuturakulov.mybudgetbackend

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ru.iuturakulov.mybudgetbackend.database.configureDatabase
import ru.iuturakulov.mybudgetbackend.plugins.*

fun main() {
    val config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    val port = System.getenv("SERVER_PORT")?.toInt() ?: 8080
    val host = config.property("ktor.deployment.host").getString()

    embeddedServer(Netty, port = port, host = host) {
        configureDatabase(config)
        configureBasic()
        configureDI()
        configureRequestValidation()
        configureAuth()
        configureSwagger()
        configureStatusPage()
        configureRoutes()
    }.start(wait = true)
}
