package ru.iuturakulov.mybudgetbackend.models.fcm

import kotlinx.serialization.Serializable

@Serializable
data class InvitationPreferencesRequest(
    val types: List<String>
)
