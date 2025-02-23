package ru.iuturakulov.mybudgetbackend.entities.participants

import ru.iuturakulov.mybudgetbackend.models.UserRole

data class ParticipantEntity(
    val id: String?, // ID участника (UUID)
    val projectId: String,      // ID проекта
    val userId: String,         // ID пользователя
    val name: String,           // Имя участника
    val email: String,          // Email участника
    val role: UserRole,         // Роль участника (OWNER, EDITOR, VIEWER)
    val createdAt: Long = System.currentTimeMillis() // Дата добавления
)
