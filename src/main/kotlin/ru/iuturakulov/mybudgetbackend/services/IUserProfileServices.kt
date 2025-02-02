package ru.iuturakulov.mybudgetbackend.services

import ru.iuturakulov.mybudgetbackend.entities.user.UserProfile
import ru.iuturakulov.mybudgetbackend.models.user.body.UserProfileBody


interface IUserProfileServices {
    suspend fun getProfile(userId: String): UserProfile
    suspend fun updateProfileInfo(userId: String, userProfile: UserProfileBody?): UserProfile
//    suspend fun updateProfileImage(userId: String, profileImage: String?): UserProfile
}