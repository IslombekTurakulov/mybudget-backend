package ru.iuturakulov.mybudgetbackend.controller.user

import io.ktor.http.*
import ru.iuturakulov.mybudgetbackend.config.JwtConfig
import ru.iuturakulov.mybudgetbackend.config.TokensResponse
import ru.iuturakulov.mybudgetbackend.database.DataBaseTransaction
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.callRequest
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.generatePassword
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponse
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.PasswordHasher
import ru.iuturakulov.mybudgetbackend.models.response.LoginResponse
import ru.iuturakulov.mybudgetbackend.models.user.body.ChangePasswordRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.ForgetPasswordEmailRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.RefreshTokenRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.RegistrationRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.VerifyEmailRequest
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import ru.iuturakulov.mybudgetbackend.services.EmailService
import ru.iuturakulov.mybudgetbackend.services.EmailTemplates
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

const val DEFAULT_SPAM_INTERVAL_SEC = 60L

interface DateTimeProvider {
    fun now(): Instant
}

class SystemDateTimeProvider : DateTimeProvider {
    override fun now(): Instant = Instant.now()
}

class RateLimiter(private val intervalSec: Long, private val clock: DateTimeProvider) {
    private val lastRequests = ConcurrentHashMap<String, Instant>()

    fun check(key: String) {
        val now = clock.now()
        val last = lastRequests[key]
        if (last != null && Duration.between(last, now).seconds < intervalSec) {
            throw AppException.Common("Слишком частые запросы. Попробуйте позже.")
        }
        lastRequests[key] = now
    }
}

class UserController(
    private val userRepo: UserRepository,
    private val emailService: EmailService,
    private val clock: DateTimeProvider = SystemDateTimeProvider(),
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    private val limiter = RateLimiter(DEFAULT_SPAM_INTERVAL_SEC, clock)

    suspend fun register(req: RegistrationRequest): String = callRequest {
        userRepo.getUserByEmail(req.email)?.let {
            throw AppException.AlreadyExists.Email("Почта уже используется")
        }
        val saved = userRepo.createUser(req)
        val emailContent = EmailTemplates.registration(req.name, req.email, Instant.ofEpochMilli(saved.createdAt), zone)
        emailService.sendEmail(req.email, emailContent.subject, emailContent.body)
        "Регистрация успешна. Проверьте почту для подтверждения."
    }

    suspend fun login(req: LoginRequest): LoginResponse = callRequest {
        val user = userRepo.getUserByEmail(req.email)
            ?: throw AppException.NotFound.User("Пользователь не найден")
        userRepo.loginUser(req)
    }

    suspend fun verifyEmail(req: VerifyEmailRequest): String = callRequest {
        val status = userRepo.verifyEmailCode(req.email, req.verificationCode)
        if (status == DataBaseTransaction.NOT_FOUND) {
            throw AppException.InvalidProperty.EmailNotExist("Неверный код подтверждения")
        }
        "Email подтвержден"
    }

    suspend fun requestPasswordReset(req: ForgetPasswordEmailRequest): String = callRequest {
        userRepo.getUserByEmail(req.email)?.let { user ->
            limiter.check(req.email)
            val newPass = generatePassword()
            userRepo.saveNewPasswordForUser(user.email, PasswordHasher.hash(newPass))
            val emailContent = EmailTemplates.passwordReset(user.email, newPass)
            emailService.sendEmail(user.email, emailContent.subject, emailContent.body)
            "Новый пароль отправлен на email"
        } ?: throw AppException.NotFound.User("Пользователь не найден")
    }

    suspend fun changePassword(userId: String, req: ChangePasswordRequest): ApiResponse<String> = callRequest {
        val user = userRepo.getUserById(userId)
            ?: throw AppException.NotFound.User("Пользователь не найден")
        if (!PasswordHasher.verify(req.oldPassword, user.password)) {
            throw AppException.InvalidProperty.PasswordNotMatch("Старый пароль неверен")
        }
        val hashed = PasswordHasher.hash(req.newPassword)
        userRepo.updateUserPassword(userId, hashed)
        val emailContent = EmailTemplates.passwordChange(user.name, user.email, req.newPassword, clock.now(), zone)
        emailService.sendEmail(user.email, emailContent.subject, emailContent.body)
        ApiResponseState.success("Пароль изменён", HttpStatusCode.OK)
    }

    suspend fun refreshToken(req: RefreshTokenRequest): TokensResponse = callRequest {
        val decoded = try {
            JwtConfig.verify(req.refreshToken)
        } catch (e: Exception) {
            throw AppException.Authentication("Недействительный или просроченный refresh‑токен")
        }

        val userId = decoded?.getClaim("userId")?.asString()
            ?: throw AppException.Authentication("Не удалось извлечь userId из токена")

        val user = userRepo.getUserById(userId)
            ?: throw AppException.NotFound.User("Пользователь не найден")

        val newAccessToken  = JwtConfig.generateToken(userId)
        val newRefreshToken = JwtConfig.generateToken(userId)

        TokensResponse(newAccessToken, newRefreshToken)
    }
}

