package ru.iuturakulov.mybudgetbackend.entities.user

import org.jetbrains.exposed.dao.id.EntityID
import ru.iuturakulov.mybudgetbackend.controller.JwtController
import ru.iuturakulov.mybudgetbackend.entities.BaseIntEntity
import ru.iuturakulov.mybudgetbackend.entities.BaseIntEntityClass
import ru.iuturakulov.mybudgetbackend.entities.BaseIntIdTable
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

object UserTable : BaseIntIdTable("user") {
    val email = varchar("email", 50)
    val password = varchar("password", 200)
    val emailVerifiedAt = text("email_verified_at").nullable()
    val rememberToken = varchar("remember_token", 50).nullable()
    val verificationCode = varchar("verification_code", 30).nullable()
    val isVerified = bool("is_verified").nullable()
    override val primaryKey = PrimaryKey(id)
}

class UsersEntity(id: EntityID<String>) : BaseIntEntity(id, UserTable) {
    companion object : BaseIntEntityClass<UsersEntity>(UserTable)

    var email by UserTable.email
    var password by UserTable.password
    var emailVerifiedAt by UserTable.emailVerifiedAt
    var rememberToken by UserTable.rememberToken
    var verificationCode by UserTable.verificationCode
    var isVerified by UserTable.isVerified
    fun response() = UsersResponse(
        id.value,
        email,
        emailVerifiedAt,
        rememberToken,
        isVerified,
    )

    fun loggedInWithToken() = LoginResponse(
        response(), JwtController.tokenProvider(JwtTokenBody(id.value, email))
    )
}

data class UsersResponse(
    val id: String,
    val email: String,
    val emailVerifiedAt: String?,
    val rememberToken: String?,
    val isVerified: Boolean?,
)

data class LoginResponse(val user: UsersResponse?, val accessToken: String)
data class ChangePassword(val oldPassword: String, val newPassword: String)
data class VerificationCode(val verificationCode: String)
