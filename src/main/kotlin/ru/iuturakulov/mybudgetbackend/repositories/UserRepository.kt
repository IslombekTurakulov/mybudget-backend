package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.config.JwtConfig
import ru.iuturakulov.mybudgetbackend.database.DataBaseTransaction
import ru.iuturakulov.mybudgetbackend.entities.user.UserEntity
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.PasswordHasher
import ru.iuturakulov.mybudgetbackend.models.response.LoginResponse
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.RegistrationRequest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UserRepository {

    private val failedResetAttempts = ConcurrentHashMap<String, Int>()
    private val lastPasswordResetRequest = ConcurrentHashMap<String, Long>()

    /**
     * **Получение пользователя по email**
     */
    fun getUserByEmail(email: String): UserEntity? = transaction {
        UserTable.selectAll().where { UserTable.email eq email.lowercase(Locale.getDefault()) }
            .mapNotNull { row -> UserEntity.fromRow(row) }
            .singleOrNull()
    }

    /**
     * **Получение пользователя по ID**
     */
    fun getUserById(userId: String): UserEntity? = transaction {
        UserTable.selectAll().where { UserTable.id eq userId }
            .mapNotNull { row -> UserEntity.fromRow(row) }
            .singleOrNull()
    }

    /**
     * **Создание нового пользователя**
     */
    fun createUser(request: RegistrationRequest): UserEntity = transaction {
        if (getUserByEmail(request.email) != null) {
            throw AppException.AlreadyExists.Email()
        }

        val hashedPassword = PasswordHasher.hash(request.password)

        val currentTimeMillis = System.currentTimeMillis()
        val userId = UUID.randomUUID().toString()

        UserTable.insert {
            it[id] = userId
            it[email] = request.email.lowercase(Locale.getDefault())
            it[password] = hashedPassword
            it[name] = request.name
            // TODO: verify email routing
            it[isEmailVerified] = true
            it[createdAt] = System.currentTimeMillis()
        }

        return@transaction UserEntity(
            id = userId,
            name = request.name,
            email = request.email,
            password = "", // Пароль не передаем обратно!
            isEmailVerified = false,
            createdAt = currentTimeMillis
        )
    }

    /**
     * **Авторизация пользователя (логин)**
     */
    fun loginUser(request: LoginRequest): LoginResponse {

        val user = getUserByEmail(request.email) ?: throw AppException.NotFound.User()

        if (!PasswordHasher.verify(request.password, user.password)) {
            throw AppException.InvalidProperty.PasswordNotMatch("Неверный пароль")
        }

//        if (!user.isEmailVerified) {
//            throw AppException.Authentication("Email не подтвержден. Проверьте почту.")
//        }

        return LoginResponse(
            token = JwtConfig.generateToken(user.id)
        )
    }

    /**
     * **Обновление пароля**
     */
    fun updateUserPassword(userId: String, newHashedPassword: String): Boolean = transaction {
        UserTable.update({ UserTable.id eq userId }) {
            it[password] = newHashedPassword
        } > 0
    }

    /**
     * **Сохранение кода подтверждения email**
     */
    fun saveEmailVerificationCode(email: String, code: String) = transaction {
        UserTable.update({ UserTable.email eq email.lowercase(Locale.getDefault()) }) {
            it[emailVerificationCode] = code
        }
    }

    /**
     * **Подтверждение кода email**
     */
    fun verifyEmailCode(email: String, code: String): DataBaseTransaction = transaction {
        val user = getUserByEmail(email) ?: return@transaction DataBaseTransaction.FAILED

        if (user.emailVerificationCode == code) {
            UserTable.update({ UserTable.email eq email }) {
                it[isEmailVerified] = true
            }
            return@transaction DataBaseTransaction.UPDATED
        }
        return@transaction DataBaseTransaction.FAILED
    }

    /**
     * **Сохранение кода сброса пароля**
     */
    fun savePasswordResetCode(email: String, code: String) = transaction {
        UserTable.update({ UserTable.email eq email.lowercase(Locale.getDefault()) }) {
            it[passwordResetCode] = code
        }
    }

    /**
     * **Проверка кода сброса пароля**
     */
    fun verifyPasswordResetCode(email: String, code: String, newPassword: String): Boolean = transaction {
        val user = getUserByEmail(email) ?: throw AppException.InvalidProperty.EmailNotExist()

        if (user.passwordResetCode == code) {
            val newHashedPassword = PasswordHasher.hash(newPassword)
            updateUserPassword(user.id, newHashedPassword)
            return@transaction true
        }
        return@transaction false
    }

    /**
     * **Проверка неудачных попыток сброса пароля**
     */
    private fun getFailedResetAttempts(email: String): Int = failedResetAttempts.getOrDefault(email, 0)

    fun incrementFailedResetAttempts(email: String) {
        failedResetAttempts[email] = getFailedResetAttempts(email) + 1
    }

    fun resetFailedResetAttempts(email: String) {
        failedResetAttempts.remove(email)
    }

    /**
     * **Последняя попытка сброса пароля**
     */
    fun getLastPasswordResetRequest(email: String): Long? = lastPasswordResetRequest[email]

    fun updateLastPasswordResetRequest(email: String, time: Long) {
        lastPasswordResetRequest[email] = time
    }
}