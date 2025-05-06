package ru.iuturakulov.mybudgetbackend.controller.user

import io.ktor.http.*
import ru.iuturakulov.mybudgetbackend.config.JwtConfig
import ru.iuturakulov.mybudgetbackend.config.TokensResponse
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
import ru.iuturakulov.mybudgetbackend.models.user.body.generate4DigitCode
import ru.iuturakulov.mybudgetbackend.repositories.EmailVerificationRepository
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import ru.iuturakulov.mybudgetbackend.services.EmailService
import ru.iuturakulov.mybudgetbackend.services.EmailTemplates
import ru.iuturakulov.mybudgetbackend.services.VerifiedEmailCache
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
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
    private val emailVerificationRepository: EmailVerificationRepository,
    private val emailService: EmailService,
    private val verifiedEmailsCache: VerifiedEmailCache,
    private val clock: DateTimeProvider = SystemDateTimeProvider(),
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    private val limiter = RateLimiter(5, clock)

    suspend fun sendVerificationCode(email: String): String = callRequest {
        userRepo.getUserByEmail(email)?.let {
            throw AppException.AlreadyExists.Email("Пользователь с такой почтой уже существует")
        }

        val user = userRepo.getUserByEmail(email)
        if (user != null && user.isEmailVerified) {
            throw AppException.AlreadyExists.Email("Email уже подтверждён")
        }

        limiter.check(email)

        val code = generate4DigitCode()
        emailVerificationRepository.saveVerificationCode(email, code)

        val emailContent = EmailTemplates.verification(email, code, forReset = false)
        emailService.sendEmail(
            toEmail = email,
            subject = emailContent.subject,
            message = emailContent.body
        )

        "Код подтверждения отправлен"
    }

    suspend fun verifyEmailCode(req: RegistrationRequest): String = callRequest {
        val success = emailVerificationRepository.verifyCode(req.email, req.code)
        if (!success) throw AppException.InvalidProperty.EmailNotExist("Неверный код подтверждения")

        // добавить email в in-memory storage или базу, пометив как подтвержденный
        verifiedEmailsCache.markVerified(req.email)
        emailVerificationRepository.removeVerificationCode(req.email)

        register(req)

        "Email подтвержден"
    }

    suspend fun verifyPasswordResetCode(email: String, code: String): String = callRequest {
        val user = userRepo.getUserByEmail(email)
            ?: throw AppException.NotFound.User("Пользователь не найден")

        // Проверяем код
        if (user.passwordResetCode != code) {
            throw AppException.InvalidProperty.PasswordResetCode("Неверный код сброса пароля")
        }

        requestPasswordReset(ForgetPasswordEmailRequest(email))
        "Код сброса пароля подтверждён."
    }

    fun register(req: RegistrationRequest) {
        if (!verifiedEmailsCache.isVerified(req.email)) {
            throw AppException.InvalidProperty.EmailNotVerified("Подтвердите email перед регистрацией")
        }

        userRepo.getUserByEmail(req.email)?.let {
            throw AppException.AlreadyExists.Email("Почта уже используется")
        }

        val saved = userRepo.createUser(req)

        val emailContent =
            EmailTemplates.registration(req.name, req.email, Instant.ofEpochMilli(saved.createdAt), zone)
        emailService.sendEmail(req.email, emailContent.subject, emailContent.body)

        verifiedEmailsCache.remove(req.email)
    }

    suspend fun login(req: LoginRequest): LoginResponse = callRequest {
        val user = userRepo.getUserByEmail(req.email)
            ?: throw AppException.NotFound.User("Пользователь не найден")

        userRepo.loginUser(req)
    }

    suspend fun sendPasswordResetCode(email: String): String = callRequest {
        val user = userRepo.getUserByEmail(email)
            ?: throw AppException.NotFound.User("Пользователь не найден")

        limiter.check(email) // Защита от спама

        // Генерация случайного кода сброса пароля
        val resetCode = generate4DigitCode()

        // Сохраняем код сброса пароля в базе данных
        userRepo.savePasswordResetCode(email, resetCode)

        // Отправка кода сброса на почту
        val content = EmailTemplates.verification(email, resetCode, forReset = true)
        emailService.sendEmail(email, content.subject, content.body)

        "Код для сброса пароля отправлен на вашу почту."
    }

    fun requestPasswordReset(req: ForgetPasswordEmailRequest) {
        userRepo.getUserByEmail(req.email)?.let { user ->
            limiter.check(req.email)

            val newPass = generatePassword()

            userRepo.saveNewPasswordForUser(
                email = user.email,
                newPassword = PasswordHasher.hash(newPass)
            )

            userRepo.clearPasswordResetCode(user.email)

            val emailContent = EmailTemplates.passwordReset(user.email, newPass)

            emailService.sendEmail(user.email, emailContent.subject, emailContent.body)
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

        val newAccessToken = JwtConfig.generateToken(userId)
        val newRefreshToken = JwtConfig.generateToken(userId)

        TokensResponse(newAccessToken, newRefreshToken)
    }
}

