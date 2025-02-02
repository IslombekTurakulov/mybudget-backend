package ru.iuturakulov.mybudgetbackend.controller.user

import ru.iuturakulov.mybudgetbackend.entities.user.UserProfile
import ru.iuturakulov.mybudgetbackend.entities.user.UserProfileTable
import ru.iuturakulov.mybudgetbackend.entities.user.UsersProfileEntity
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.query
import ru.iuturakulov.mybudgetbackend.extensions.UserNotFoundException
import ru.iuturakulov.mybudgetbackend.models.user.body.UserProfileBody
import ru.iuturakulov.mybudgetbackend.services.IUserProfileServices

class UserProfileController : IUserProfileServices {

    override suspend fun getProfile(userId: String): UserProfile = query {
        val isProfileExist = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        isProfileExist?.response() ?: throw UserNotFoundException(message = "$userId")
    }

    override suspend fun updateProfileInfo(userId: String, userProfile: UserProfileBody?): UserProfile = query {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.userId eq userId }.toList().singleOrNull()
        userProfileEntity?.let {
            it.firstName = userProfile?.firstName ?: it.firstName
            it.lastName = userProfile?.lastName ?: it.lastName
            it.userDescription = userProfile?.userDescription ?: it.userDescription
            it.response()
        } ?: throw UserNotFoundException()
    }
}