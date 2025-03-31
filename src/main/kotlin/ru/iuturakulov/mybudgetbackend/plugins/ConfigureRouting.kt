package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.iuturakulov.mybudgetbackend.controller.analytics.AnalyticsController
import ru.iuturakulov.mybudgetbackend.controller.notification.NotificationController
import ru.iuturakulov.mybudgetbackend.controller.project.ProjectController
import ru.iuturakulov.mybudgetbackend.controller.settings.SettingsController
import ru.iuturakulov.mybudgetbackend.controller.transaction.TransactionController
import ru.iuturakulov.mybudgetbackend.controller.user.UserController
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.routing.analyticsRoute
import ru.iuturakulov.mybudgetbackend.routing.notificationRoute
import ru.iuturakulov.mybudgetbackend.routing.projectRoute
import ru.iuturakulov.mybudgetbackend.routing.settingsRoute
import ru.iuturakulov.mybudgetbackend.routing.transactionRoute
import ru.iuturakulov.mybudgetbackend.routing.userRoute

fun Application.configureRoutes() {
    val userController: UserController by inject()
    val projectController: ProjectController by inject()
    val notificationController: NotificationController by inject()
    val transactionController: TransactionController by inject()
    val analyticsController: AnalyticsController by inject()
    val settingsController: SettingsController by inject()
    val auditLogService: AuditLogService by inject()

    routing {
        userRoute(userController = userController)
        projectRoute(projectController = projectController, auditLogService = auditLogService)
        transactionRoute(transactionController = transactionController, auditLogService = auditLogService)
        notificationRoute(notificationController = notificationController)
        analyticsRoute(analyticsController = analyticsController)
        settingsRoute(settingsController = settingsController)
    }
}
