package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.iuturakulov.mybudgetbackend.controller.user.UserController
import ru.iuturakulov.mybudgetbackend.controller.user.UserProfileController
import ru.iuturakulov.mybudgetbackend.routing.userProfileRoute
import ru.iuturakulov.mybudgetbackend.routing.userRoute

fun Application.configureRoute() {
    val userController: UserController by inject()
    val userProfileController: UserProfileController by inject()
    routing {
        userRoute(userController)
        userProfileRoute(userProfileController)
    }
}
