package ru.iuturakulov.mybudgetbackend.extensions

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {

    private const val COST = 12  // Количество раундов хеширования (чем больше, тем безопаснее, но медленнее)

    /**
     * Хеширование пароля
     */
    fun hash(password: String): String {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray())
    }

    /**
     * Проверка пароля с хешем
     */
    fun verify(password: String, hashedPassword: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified
    }
}