package ru.iuturakulov.mybudgetbackend.controller.settings

import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.callRequest
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.settings.UserSettingsRequest
import ru.iuturakulov.mybudgetbackend.models.settings.UserSettingsResponse
import ru.iuturakulov.mybudgetbackend.repositories.SettingsRepository

class SettingsController(private val settingsRepository: SettingsRepository) {

    suspend fun getUserSettings(userId: String): UserSettingsResponse = callRequest {
        settingsRepository.getUserSettings(userId)
            ?: throw AppException.NotFound.User("Настройки пользователя не найдены")
    }

    suspend fun updateUserSettings(
        userId: String,
        request: UserSettingsRequest
    ): UserSettingsResponse = callRequest {
        settingsRepository.updateUserSettings(userId, request)
        settingsRepository.getUserSettings(userId)
            ?: throw AppException.NotFound.User("Ошибка обновления настроек")
    }
}