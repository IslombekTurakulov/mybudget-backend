package ru.iuturakulov.mybudgetbackend.models.fcm

import kotlinx.serialization.Serializable

@Serializable
data class InvitationPreferencesResponse(
    val types: List<String>
)