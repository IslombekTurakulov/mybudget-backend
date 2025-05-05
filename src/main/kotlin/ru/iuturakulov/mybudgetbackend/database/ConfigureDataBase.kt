package ru.iuturakulov.mybudgetbackend.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.audit.AuditLogTable
import ru.iuturakulov.mybudgetbackend.entities.fcm.DeviceTokens
import ru.iuturakulov.mybudgetbackend.entities.invitation.InvitationTable
import ru.iuturakulov.mybudgetbackend.entities.notification.FCMNotificationTable
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationTable
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import javax.sql.DataSource

fun configureDatabase() {
    DatabaseConfig.initDB()
    transaction {
        addLogger(StdOutSqlLogger)
        create(UserTable)
        create(ProjectsTable)
        create(TransactionsTable)
        create(ParticipantTable)
        create(NotificationTable)
        create(DeviceTokens)
        create(FCMNotificationTable)
        create(InvitationTable)
        create(AuditLogTable)
    }
}

object DatabaseConfig {
    fun initDB() {
        val config = HikariConfig("/hikari.properties")
        val dataSource = HikariDataSource(config)
        runFlyway(dataSource)
        Database.connect(dataSource)
    }

    private fun runFlyway(dataSource: DataSource) {
        try {
            val flyway = Flyway
                .configure()
                .locations("classpath:db/migrations")
                .dataSource(dataSource)
                .load()
            flyway.migrate()
        } catch (e: FlywayException) {
            println("⚠ Ошибка миграции Flyway: ${e.message}")
            throw e
        }
    }
}
