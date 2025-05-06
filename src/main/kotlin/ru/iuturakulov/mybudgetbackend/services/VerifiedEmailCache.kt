package ru.iuturakulov.mybudgetbackend.services

import java.util.concurrent.ConcurrentHashMap

class VerifiedEmailCache(
    private val ttlMillis: Long = 10 * 60 * 1000 // 10 минут по умолчанию
) {

    private val cache = ConcurrentHashMap<String, Long>() // email -> expireAt

    fun markVerified(email: String) {
        val expireAt = System.currentTimeMillis() + ttlMillis
        cache[email] = expireAt
    }

    fun isVerified(email: String): Boolean {
        val now = System.currentTimeMillis()
        val expireAt = cache[email] ?: return false

        if (expireAt < now) {
            cache.remove(email)
            return false
        }

        return true
    }

    fun remove(email: String) {
        cache.remove(email)
    }

    fun clearExpired() {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { it.value < now }
    }
}
