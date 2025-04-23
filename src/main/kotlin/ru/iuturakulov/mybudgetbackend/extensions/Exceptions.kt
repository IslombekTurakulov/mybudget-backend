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
        class User(details: String? = null) : AlreadyExists("User already exists", details)
    }

    sealed class InvalidProperty(
        base: String = "Invalid property",
        details: String? = null
    ) : BaseException(base, details) {
        class Email(details: String? = null) : InvalidProperty("Email invalid", details)
        class EmailNotExist(details: String? = null) : InvalidProperty("Email does not exist", details)
        class Password(details: String? = null) : InvalidProperty("Password invalid", details)
        class Transaction(details: String? = null) : InvalidProperty("Transaction invalid", details)
        class Project(details: String? = null) : InvalidProperty("Project invalid", details)
        class PasswordNotMatch(details: String? = null) : InvalidProperty("Password does not match", details)
    }

    sealed class NotFound(
        base: String = "Not found",
        details: String? = null
    ) : BaseException(base, details) {
        class User(details: String? = null) : NotFound("User not found", details)
        class Project(details: String? = null) : NotFound("Project not found", details)
        class Transaction(details: String? = null) : NotFound("Transaction not found", details)
        class Resource(details: String? = null) : NotFound("Resource not found", details)
    }

    class Authorization(
        details: String? = null
    ) : BaseException("No authority for this action", details)

    class Authentication(
        details: String? = null
    ) : BaseException("Invalid authentication", details)

    class RateLimitExceeded(
        details: String? = null
    ) : BaseException("Too many requests", details)

    class InvalidToken(
        details: String? = null
    ) : BaseException("Invalid or expired token", details)

    class ActionNotAllowed(
        details: String? = null
    ) : BaseException("Action not allowed", details)

    class DatabaseError(
        details: String? = null,
        cause: Throwable? = null
    ) : BaseException("Database error", details, cause)

    class Common(
        details: String? = null,
        cause: Throwable? = null
    ) : BaseException("Something went wrong", details, cause)
}