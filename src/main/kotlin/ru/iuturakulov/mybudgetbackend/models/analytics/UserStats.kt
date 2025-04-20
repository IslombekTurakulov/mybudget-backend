package ru.iuturakulov.mybudgetbackend.models.analytics

data class UserStats(
    val userId: String,
    val userName: String,
    val amount: Double,
    val count: Int
)