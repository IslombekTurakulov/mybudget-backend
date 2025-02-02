package ru.iuturakulov.mybudgetbackend.controller.user

import at.favre.lib.crypto.bcrypt.BCrypt
import ru.iuturakulov.mybudgetbackend.models.user.response.RegistrationResponse
import ru.iuturakulov.mybudgetbackend.entities.user.ChangePassword
import ru.iuturakulov.mybudgetbackend.entities.user.LoginResponse
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.entities.user.UsersEntity
import ru.iuturakulov.mybudgetbackend.entities.user.UsersProfileEntity
import ru.iuturakulov.mybudgetbackend.entities.user.VerificationCode
import ru.iuturakulov.mybudgetbackend.extensions.AlreadyExistsException
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.query
import ru.iuturakulov.mybudgetbackend.extensions.DataBaseTransaction
import ru.iuturakulov.mybudgetbackend.extensions.PasswordNotMatchException
import ru.iuturakulov.mybudgetbackend.extensions.UserNotFoundException
import ru.iuturakulov.mybudgetbackend.models.user.body.ConfirmPassword
import ru.iuturakulov.mybudgetbackend.models.user.body.ForgetPasswordEmail
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginBody
import ru.iuturakulov.mybudgetbackend.models.user.body.RegistrationBody
import ru.iuturakulov.mybudgetbackend.services.IUserServices
import kotlin.random.Random

class UserController : IUserServices {
    override suspend fun addUser(registrationBody: RegistrationBody): RegistrationResponse = query {
        val userEntity =
            UsersEntity.find { UserTable.email eq registrationBody.email }
                .toList().singleOrNull()
        userEntity?.let {
            AlreadyExistsException()
        }
        val inserted = UsersEntity.new {
            email = registrationBody.email
            password = BCrypt.withDefaults().hashToString(12, registrationBody.password.toCharArray())
        }
        UsersProfileEntity.new {
            userId = inserted.id
        }
        RegistrationResponse(inserted.id.value, registrationBody.email)
    }

    override suspend fun login(loginBody: LoginBody): LoginResponse = query {
        val userEntity =
            UsersEntity.find { UserTable.email eq loginBody.email }
                .toList().singleOrNull()
        userEntity?.let {
            if (BCrypt.verifyer().verify(
                    loginBody.password.toCharArray(), it.password
                ).verified
            ) {
                it.loggedInWithToken()
            } else {
                throw PasswordNotMatchException()
            }
        } ?: throw UserNotFoundException()
    }

    override suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean = query {
        val userEntity = UsersEntity.find { UserTable.id eq userId }.toList().singleOrNull()
        userEntity?.let {
            if (BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), it.password).verified) {
                it.password = BCrypt.withDefaults().hashToString(12, changePassword.newPassword.toCharArray())
                true
            } else {
                false
            }
        } ?: throw UserNotFoundException()
    }

    override suspend fun forgetPasswordSendCode(forgetPasswordBody: ForgetPasswordEmail): VerificationCode = query {
        val userEntity = UsersEntity.find { UserTable.email eq forgetPasswordBody.email }.toList().singleOrNull()
        userEntity?.let {
            val verificationCode = Random.nextInt(1000, 9999).toString()
            it.verificationCode = verificationCode
            VerificationCode(verificationCode)
        } ?: throw UserNotFoundException()
    }

    override suspend fun forgetPasswordVerificationCode(confirmPasswordBody: ConfirmPassword): Int = query {
        val userEntity = UsersEntity.find { UserTable.email eq confirmPasswordBody.email }.toList().singleOrNull()
        userEntity?.let {
            if (confirmPasswordBody.verificationCode == it.verificationCode) {
                it.password = BCrypt.withDefaults().hashToString(12, confirmPasswordBody.newPassword.toCharArray())
                it.verificationCode = null
                DataBaseTransaction.FOUND
            } else {
                DataBaseTransaction.NOT_FOUND
            }
        } ?: throw UserNotFoundException()
    }

}
