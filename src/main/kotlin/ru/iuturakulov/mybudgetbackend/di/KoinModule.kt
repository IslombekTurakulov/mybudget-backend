package ru.iuturakulov.mybudgetbackend.di

import org.koin.dsl.module
import ru.iuturakulov.mybudgetbackend.controller.analytics.AnalyticsController
import ru.iuturakulov.mybudgetbackend.controller.notification.NotificationController
import ru.iuturakulov.mybudgetbackend.controller.project.ProjectController
import ru.iuturakulov.mybudgetbackend.controller.transaction.TransactionController
import ru.iuturakulov.mybudgetbackend.controller.user.UserController
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.repositories.AnalyticsRepository
import ru.iuturakulov.mybudgetbackend.repositories.AuditLogRepository
import ru.iuturakulov.mybudgetbackend.repositories.NotificationRepository
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.TransactionRepository
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import services.EmailService
import services.InvitationService
import services.NotificationService

val repositoryModule = module {
    single { UserRepository() }
    single { ProjectRepository() }
    single { ParticipantRepository() }
    single { TransactionRepository() }
    single { NotificationRepository() }
    single { AuditLogRepository() }
    single { AnalyticsRepository() }
}

val serviceModule = module {
    single { NotificationService(get()) }
    single { AuditLogService(get()) }
    single { InvitationService() }
    single { EmailService() }
    single { AccessControl() }
}

val controllerModule = module {
    single { UserController(get(), get()) }
    single { ProjectController(get(), get(), get(), get(), get(), get()) }
    single { NotificationController(get()) }
    single { TransactionController(get(), get(), get(), get(), get(), get()) }
    single { AnalyticsController(get(), get(), get()) }
}

val appModule = listOf(repositoryModule, serviceModule, controllerModule)