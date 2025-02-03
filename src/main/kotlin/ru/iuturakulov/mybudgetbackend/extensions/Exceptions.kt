package ru.iuturakulov.mybudgetbackend.extensions

sealed class AppException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    abstract class BaseException(
        base: String,
        details: String? = null,
        cause: Throwable? = null
    ) : AppException("$base${details?.let { ": $it" } ?: ""}", cause)

    sealed class AlreadyExists(
        base: String = "Already exists",
        details: String? = null
    ) : BaseException(base, details) {
        class Email(details: String? = null) : AlreadyExists("Email already in use", details)
    }

    sealed class InvalidProperty(
        base: String = "Invalid property",
        details: String? = null
    ) : BaseException(base, details) {
        class Email(details: String? = null) : InvalidProperty("Email invalid", details)
        class EmailNotExist(details: String? = null) : InvalidProperty("Email not exist", details)
        class Password(details: String? = null) : InvalidProperty("Password invalid", details)
        class PasswordNotMatch(details: String? = null) : InvalidProperty("Password not match", details)
    }

    sealed class NotFound(
        base: String = "Not found",
        details: String? = null
    ) : BaseException(base, details) {
        class User(details: String? = null) : NotFound("User not found", details)
    }

    class Authorization(
        details: String? = null
    ) : BaseException("No authority for this action", details)

    class Authentication(
        details: String? = null
    ) : BaseException("Invalid authentication", details)

    class Common(
        details: String? = null,
        cause: Throwable? = null
    ) : BaseException("Something wrong happened", details, cause)
}