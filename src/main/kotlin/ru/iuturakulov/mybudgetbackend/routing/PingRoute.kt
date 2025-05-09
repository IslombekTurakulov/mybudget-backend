package ru.iuturakulov.mybudgetbackend.routing

import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PingRoute")

fun Route.pingRoute() {
    head("/ping") {
        logger.info("Received HEAD request to /ping")
        call.respondText("") // Пустое тело ответа для HEAD запроса
    }

    get("/ping") {
        logger.info("Received GET request to /ping")
        call.respondText("pong") // Для тестирования
    }
}
