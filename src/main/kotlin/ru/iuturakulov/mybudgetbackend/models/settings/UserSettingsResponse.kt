package ru.iuturakulov.mybudgetbackend.models.settings

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsResponse(
    val name: String,
    val email: String,
    val language: String,
    val notificationsEnabled: Boolean,
    val darkThemeEnabled: Boolean,
)