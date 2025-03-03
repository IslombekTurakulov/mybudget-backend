package ru.iuturakulov.mybudgetbackend.controller.user

import io.ktor.http.*
import ru.iuturakulov.mybudgetbackend.config.JwtConfig
import ru.iuturakulov.mybudgetbackend.database.DataBaseTransaction
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.callRequest
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.generateVerificationCode
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponse
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.PasswordHasher
import ru.iuturakulov.mybudgetbackend.models.user.body.ChangePasswordRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.ForgetPasswordEmailRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.RefreshTokenRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.RegistrationRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.VerifyEmailRequest
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import services.EmailService
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private const val DURATION_BRUTE_FORCE_SPAM = 60

// TODO: Implement audit service
class UserController(private val userRepository: UserRepository, private val emailService: EmailService) {

    private val passwordResetCooldown = ConcurrentHashMap<String, Long>()

    /**
     * Регистрация нового пользователя
     */
    suspend fun register(request: RegistrationRequest): ApiResponse<String> = callRequest {
        val existingUser = userRepository.getUserByEmail(request.email)
        if (existingUser != null) throw AppException.AlreadyExists.Email("Почта уже используется")
        val savedUser = userRepository.createUser(request)

        // Генерируем код верификации email и отправляем пользователю
        val verificationCode = generateVerificationCode()
        userRepository.saveEmailVerificationCode(savedUser.email, verificationCode)
        emailService.sendEmail(savedUser.email, "Подтвердите регистрацию", "Ваш код: $verificationCode")

        return@callRequest ApiResponseState.success(
            "Регистрация успешна. Проверьте почту для подтверждения.",
            HttpStatusCode.Created
        )
    }

    /**
     * Вход пользователя
     */
    suspend fun login(request: LoginRequest): ApiResponse<String> = callRequest {
        val token = userRepository.loginUser(request)
        return@callRequest ApiResponseState.success(token, HttpStatusCode.OK)
    }

    /**
     * Подтверждение email
     */
    suspend fun verifyEmail(request: VerifyEmailRequest): ApiResponse<String> = callRequest {
        val verificationStatus = userRepository.verifyEmailCode(request.email, request.verificationCode)
        if (verificationStatus == DataBaseTransaction.NOT_FOUND) {
            throw AppException.InvalidProperty.EmailNotExist("Неверный код подтверждения")
        }
        return@callRequest ApiResponseState.success("Email подтвержден", HttpStatusCode.OK)
    }

    /**
     * Запрос на восстановление пароля
     */
    suspend fun requestPasswordReset(request: ForgetPasswordEmailRequest): ApiResponse<String> = callRequest {
        val user = userRepository.getUserByEmail(request.email)
            ?: throw AppException.NotFound.User("Пользователь с таким email не найден")

        // Проверяем частоту запросов сброса пароля (1 минута)
        val lastRequestTime = passwordResetCooldown[request.email] ?: 0L
        if (Instant.now().epochSecond - lastRequestTime < DURATION_BRUTE_FORCE_SPAM) {
            throw AppException.Common("Слишком частые запросы сброса пароля. Подождите минуту.")
        }

        val resetCode = generateVerificationCode()
        userRepository.savePasswordResetCode(user.email, resetCode)
        emailService.sendEmail(user.email, "Восстановление пароля", "Ваш код: $resetCode")

        passwordResetCooldown[request.email] = Instant.now().epochSecond

        return@callRequest ApiResponseState.success("Код для сброса пароля отправлен на email", HttpStatusCode.OK)
    }


    /**
     * Смена пароля
     */
    suspend fun changePassword(userId: String, request: ChangePasswordRequest): ApiResponse<String> = callRequest {
        val user = userRepository.getUserById(userId)
            ?: throw AppException.NotFound.User("Пользователь не найден")

        if (!PasswordHasher.verify(request.oldPassword, user.password)) {
            throw AppException.InvalidProperty.PasswordNotMatch("Старый пароль неверен")
        }

        val hashedNewPassword = PasswordHasher.hash(request.newPassword)
        userRepository.updateUserPassword(userId, hashedNewPassword)

        return@callRequest ApiResponseState.success("Пароль изменен", HttpStatusCode.OK)
    }

    suspend fun refreshToken(request: RefreshTokenRequest): ApiResponse<String> = callRequest {
        val decodedJWT = try {
            JwtConfig.verify(request.refreshToken)
        } catch (e: Exception) {
            throw AppException.Authentication("Недействительный refresh-токен")
        }

        val userId = decodedJWT?.getClaim("userId")?.asString()
            ?: throw AppException.Authentication("Ошибка верификации токена")

        val user = userRepository.getUserById(userId)
            ?: throw AppException.NotFound.User("Пользователь не найден")

        // Генерируем новый access-token
        val newAccessToken = JwtConfig.generateToken(user.id)
        return@callRequest ApiResponseState.success(newAccessToken, HttpStatusCode.OK)
    }

}
