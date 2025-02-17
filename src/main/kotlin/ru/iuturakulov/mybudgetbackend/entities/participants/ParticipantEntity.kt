package ru.iuturakulov.mybudgetbackend.entities.participants

import org.jetbrains.exposed.sql.ResultRow
import ru.iuturakulov.mybudgetbackend.models.UserRole
import java.util.*

data class ParticipantEntity(
    val id: String = UUID.randomUUID().toString(), // ID участника (UUID)
    val projectId: String,      // ID проекта
    val userId: String,         // ID пользователя
    val name: String,           // Имя участника
    val email: String,          // Email участника
    val role: UserRole,         // Роль участника (OWNER, EDITOR, VIEWER)
    val createdAt: Long = System.currentTimeMillis() // Дата добавления
)
