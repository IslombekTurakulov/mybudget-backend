package ru.iuturakulov.mybudgetbackend.entities.audit

import java.util.*

data class AuditLogEntity(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val action: String,
    val timestamp: Long
)
