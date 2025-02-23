package ru.iuturakulov.mybudgetbackend.models.project

data class AcceptInviteRequest(
    val projectId: String,
    val inviteCode: String
)
