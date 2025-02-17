package ru.iuturakulov.mybudgetbackend.models

enum class UserRole {
    OWNER,      // Владелец проекта (может всё)
    EDITOR,     // Редактор (может изменять проект, добавлять/удалять транзакции)
    VIEWER;     // Наблюдатель (только просмотр)

    companion object {
        fun fromString(role: String): UserRole? {
            return entries.find { it.name.equals(role, ignoreCase = true) }
        }
    }
}