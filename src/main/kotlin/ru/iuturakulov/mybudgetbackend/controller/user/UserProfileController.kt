package ru.iuturakulov.mybudgetbackend.controller.user

import ru.iuturakulov.mybudgetbackend.entities.user.UserProfile
import ru.iuturakulov.mybudgetbackend.entities.user.UserProfileTable
import ru.iuturakulov.mybudgetbackend.entities.user.UsersProfileEntity
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.query
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.user.body.UserProfileBody
import ru.iuturakulov.mybudgetbackend.services.IUserProfileServices

class UserProfileController : IUserProfileServices {

    override suspend fun getProfile(userId: String): UserProfile = query {
        val isProfileExist = UsersProfileEntity.find { UserProfileTable.userId eq userId }
            .singleOrNull()
            ?: throw AppException.NotFound.User("Profile not found for user: $userId")

        isProfileExist.response()
    }

    override suspend fun updateProfileInfo(userId: String, userProfile: UserProfileBody?): UserProfile = query {
        val userProfileEntity = UsersProfileEntity.find { UserProfileTable.userId eq userId }
            .singleOrNull()
            ?: throw AppException.NotFound.User("Profile not found for user: $userId")

        userProfileEntity.apply {
            firstName = userProfile?.firstName ?: firstName
            lastName = userProfile?.lastName ?: lastName
            userDescription = userProfile?.userDescription ?: userDescription
        }.response()
    }
}