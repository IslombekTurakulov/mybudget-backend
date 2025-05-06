package ru.iuturakulov.mybudgetbackend.di

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
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
import ru.iuturakulov.mybudgetbackend.repositories.DeviceTokenRepository
import ru.iuturakulov.mybudgetbackend.repositories.EmailVerificationRepository
import ru.iuturakulov.mybudgetbackend.repositories.FCMNotificationTableRepository
import ru.iuturakulov.mybudgetbackend.repositories.NotificationRepository
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.SettingsRepository
import ru.iuturakulov.mybudgetbackend.repositories.TransactionRepository
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import ru.iuturakulov.mybudgetbackend.services.EmailService
import ru.iuturakulov.mybudgetbackend.services.FcmService
import ru.iuturakulov.mybudgetbackend.services.InvitationService
import ru.iuturakulov.mybudgetbackend.services.NotificationGuard
import ru.iuturakulov.mybudgetbackend.services.NotificationManager
import ru.iuturakulov.mybudgetbackend.services.OverallNotificationService
import ru.iuturakulov.mybudgetbackend.services.VerifiedEmailCache

val repositoryModule = module {
    single { UserRepository() }
    single { ProjectRepository() }
    single { ParticipantRepository() }
    single { TransactionRepository() }
    single { NotificationRepository() }
    single { AuditLogRepository() }
    single { AnalyticsRepository() }
    single { SettingsRepository() }
    single { FCMNotificationTableRepository() }
    single { DeviceTokenRepository() }
    single { EmailVerificationRepository() }
}

val serviceModule = module {
    single { OverallNotificationService(get()) }
    single { AuditLogService(get()) }
    single { InvitationService() }
    single { EmailService() }
    single { AccessControl() }

    single { VerifiedEmailCache() }

    single<FcmService> {
        val env = HoconApplicationConfig(ConfigFactory.load("application.conf"))
        val serviceAccountPath = env.property("ktor.firebase.serviceAccountPath").getString()
        val projectId = env.property("ktor.firebase.projectId").getString()
        FcmService(serviceAccountPath, projectId)
    }

    single {
        NotificationGuard(
            deviceRepo = get(),
            projectRepo = get()
        )
    }

    single {
        NotificationManager(
            deviceRepo = get(),
            fcm = get(),
            overallNotificationService = get(),
            notificationGuard = get()
        )
    }
}

val controllerModule = module {
    single {
        UserController(
            emailVerificationRepository = get(),
            verifiedEmailsCache = get(),
            userRepo = get(),
            emailService = get(),
        )
    }
    single {
        ProjectController(
            projectRepository = get(),
            participantRepository = get(),
            accessControl = get(),
            invitationService = get(),
            fcmNotificationTableRepository = get(),
            auditLogService = get(),
            notificationManager = get(),
        )
    }

    single { NotificationController(overallNotificationService = get()) }

    single {
        TransactionController(
            transactionRepository = get(),
            projectRepository = get(),
            participantRepository = get(),
            accessControl = get(),
            auditLog = get(),
            notificationManager = get()
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