package ru.iuturakulov.mybudgetbackend.models

data class InviteResult(
    val success: Boolean,
    val message: String,
    val qrCodeBase64: String? = null,
    val inviteCode: String? = null
)
