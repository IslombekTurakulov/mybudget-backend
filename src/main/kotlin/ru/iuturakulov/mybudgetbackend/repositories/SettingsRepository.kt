package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.models.settings.UserSettingsRequest
import ru.iuturakulov.mybudgetbackend.models.settings.UserSettingsResponse

class SettingsRepository {

    fun getUserSettings(userId: String): UserSettingsResponse? = transaction {
        UserTable.selectAll().where { UserTable.id eq userId }.mapNotNull { row ->
            UserSettingsResponse(
                name = row[UserTable.name],
                email = row[UserTable.email],
                language = row[UserTable.language],
                notificationsEnabled = row[UserTable.notificationsEnabled],
                darkThemeEnabled = row[UserTable.darkThemeEnabled]
            )
        }.singleOrNull()
    }

    fun updateUserSettings(userId: String, request: UserSettingsRequest) {
        transaction {
            UserTable.update({ UserTable.id eq userId }) { statement ->
                statement[name] = request.name
                statement[language] = request.language
                statement[notificationsEnabled] = request.notificationsEnabled
                statement[darkThemeEnabled] = request.darkThemeEnabled
            }

            ParticipantTable.update({ ParticipantTable.userId eq userId }) { statement ->
                statement[name] = request.name
            }
        }
    }
}
