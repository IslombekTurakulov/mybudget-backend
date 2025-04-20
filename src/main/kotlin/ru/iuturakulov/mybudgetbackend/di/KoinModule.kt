package ru.iuturakulov.mybudgetbackend.di

import org.koin.dsl.module
import ru.iuturakulov.mybudgetbackend.controller.analytics.AnalyticsController
import ru.iuturakulov.mybudgetbackend.controller.notification.NotificationController
import ru.iuturakulov.mybudgetbackend.controller.project.ProjectController
import ru.iuturakulov.mybudgetbackend.controller.settings.SettingsController
import ru.iuturakulov.mybudgetbackend.controller.transaction.TransactionController
import ru.iuturakulov.mybudgetbackend.controller.user.UserController
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.repositories.AnalyticsRepository
import ru.iuturakulov.mybudgetbackend.repositories.AuditLogRepository
import ru.iuturakulov.mybudgetbackend.repositories.NotificationRepository
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.SettingsRepository
import ru.iuturakulov.mybudgetbackend.repositories.TransactionRepository
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import ru.iuturakulov.mybudgetbackend.services.EmailService
import ru.iuturakulov.mybudgetbackend.services.InvitationService
import ru.iuturakulov.mybudgetbackend.services.NotificationService

val repositoryModule = module {
    single { UserRepository() }
    single { ProjectRepository() }
    single { ParticipantRepository() }
    single { TransactionRepository() }
    single { NotificationRepository() }
    single { AuditLogRepository() }
    single { AnalyticsRepository() }
    single { SettingsRepository() }
}

val serviceModule = module {
    single { NotificationService(get()) }
    single { AuditLogService(get()) }
    single { InvitationService() }
    single { EmailService() }
    single { AccessControl() }
}

val controllerModule = module {
    single {
        UserController(
            userRepo = get(),
            emailService = get()
        )
    }
    single {
        ProjectController(
            projectRepository = get(),
            participantRepository = get(),
            accessControl = get(),
            invitationService = get(),
            notificationService = get(),
            auditLogService = get()
        )
    }
    single { NotificationController(notificationService = get()) }
    single {
        TransactionController(
            transactionRepository = get(),
            projectRepository = get(),
            participantRepository = get(),
            accessControl = get(),
            auditLog = get(),
            notificationService = get()
        )
    }
    single {
        AnalyticsController(
            analyticsRepository = get(),
            projectRepository = get(),
            transactionRepository = get(),
            userRepository = get(),
            emailService = get()
        )
    }
    single { SettingsController(settingsRepository = get()) }
}

val appModule = listOf(repositoryModule, serviceModule, controllerModule)