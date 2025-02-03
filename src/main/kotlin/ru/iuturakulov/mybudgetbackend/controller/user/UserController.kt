package ru.iuturakulov.mybudgetbackend.controller.user

import at.favre.lib.crypto.bcrypt.BCrypt
import ru.iuturakulov.mybudgetbackend.models.user.response.RegistrationResponse
import ru.iuturakulov.mybudgetbackend.entities.user.ChangePassword
import ru.iuturakulov.mybudgetbackend.entities.user.LoginResponse
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.entities.user.UsersEntity
import ru.iuturakulov.mybudgetbackend.entities.user.UsersProfileEntity
import ru.iuturakulov.mybudgetbackend.entities.user.VerificationCode
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.query
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.DataBaseTransaction
import ru.iuturakulov.mybudgetbackend.models.user.body.ConfirmPassword
import ru.iuturakulov.mybudgetbackend.models.user.body.ForgetPasswordEmail
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginBody
import ru.iuturakulov.mybudgetbackend.models.user.body.RegistrationBody
import ru.iuturakulov.mybudgetbackend.services.IUserServices
import kotlin.random.Random

class UserController : IUserServices {
    override suspend fun addUser(registrationBody: RegistrationBody): RegistrationResponse = query {
        val userEntity = UsersEntity.find { UserTable.email eq registrationBody.email }
            .singleOrNull()

        if (userEntity != null) {
            throw AppException.AlreadyExists.Email()
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
        val userEntity = UsersEntity.find { UserTable.email eq loginBody.email }
            .singleOrNull() ?: throw AppException.NotFound.User()

        if (!BCrypt.verifyer().verify(loginBody.password.toCharArray(), userEntity.password).verified) {
            throw AppException.InvalidProperty.PasswordNotMatch()
        }

        userEntity.loggedInWithToken()
    }

    override suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean = query {
        val userEntity = UsersEntity.find { UserTable.id eq userId }
            .singleOrNull() ?: throw AppException.NotFound.User()

        if (!BCrypt.verifyer().verify(changePassword.oldPassword.toCharArray(), userEntity.password).verified) {
            throw AppException.InvalidProperty.PasswordNotMatch()
        }

        userEntity.password = BCrypt.withDefaults().hashToString(12, changePassword.newPassword.toCharArray())
        true
    }

    override suspend fun forgetPasswordSendCode(forgetPasswordBody: ForgetPasswordEmail): VerificationCode = query {
        val userEntity = UsersEntity.find { UserTable.email eq forgetPasswordBody.email }
            .singleOrNull() ?: throw AppException.NotFound.User()

        val verificationCode = Random.nextInt(1000, 9999).toString()
        userEntity.verificationCode = verificationCode
        VerificationCode(verificationCode)
    }

    override suspend fun forgetPasswordVerificationCode(confirmPasswordBody: ConfirmPassword): Int = query {
        val userEntity = UsersEntity.find { UserTable.email eq confirmPasswordBody.email }
            .singleOrNull() ?: throw AppException.NotFound.User()

        if (confirmPasswordBody.verificationCode != userEntity.verificationCode) {
            throw AppException.InvalidProperty.Password("Invalid verification code")
        }

        userEntity.password = BCrypt.withDefaults().hashToString(12, confirmPasswordBody.newPassword.toCharArray())
        userEntity.verificationCode = null
        DataBaseTransaction.FOUND
    }
}