package ru.iuturakulov.mybudgetbackend.services

import ru.iuturakulov.mybudgetbackend.models.user.response.RegistrationResponse
import ru.iuturakulov.mybudgetbackend.entities.user.ChangePassword
import ru.iuturakulov.mybudgetbackend.entities.user.LoginResponse
import ru.iuturakulov.mybudgetbackend.entities.user.VerificationCode
import ru.iuturakulov.mybudgetbackend.models.user.body.ConfirmPassword
import ru.iuturakulov.mybudgetbackend.models.user.body.ForgetPasswordEmail
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginBody
import ru.iuturakulov.mybudgetbackend.models.user.body.RegistrationBody

interface IUserServices {
    suspend fun addUser(registrationBody: RegistrationBody): RegistrationResponse
    suspend fun login(loginBody: LoginBody): LoginResponse
    suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean
    suspend fun forgetPasswordSendCode(forgetPasswordBody: ForgetPasswordEmail): VerificationCode
    suspend fun forgetPasswordVerificationCode(confirmPasswordBody: ConfirmPassword): Int
}