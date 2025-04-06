package ru.iuturakulov.mybudgetbackend.routing

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.pingRoute() {
    head("/ping") {
        call.respondText("") // Пустое тело ответа для HEAD запроса
    }

    get("/ping") {
        call.respondText("pong") // Для тестирования
    }
}
