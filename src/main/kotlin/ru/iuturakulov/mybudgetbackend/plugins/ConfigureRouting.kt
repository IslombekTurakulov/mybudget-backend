package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.iuturakulov.mybudgetbackend.controller.participant.ParticipantController
import ru.iuturakulov.mybudgetbackend.controller.project.ProjectController
import ru.iuturakulov.mybudgetbackend.controller.user.UserController
import ru.iuturakulov.mybudgetbackend.routing.participantRoute
import ru.iuturakulov.mybudgetbackend.routing.projectRoute
import ru.iuturakulov.mybudgetbackend.routing.userRoute

fun Application.configureRoutes() {
    val userController: UserController by inject()
    val projectController: ProjectController by inject()
    val participantController: ParticipantController by inject()
    routing {
        userRoute(userController = userController)
        projectRoute(projectController = projectController)
        participantRoute(participantController = participantController)
    }
}
