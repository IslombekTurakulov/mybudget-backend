package ru.iuturakulov.mybudgetbackend.entities.user

import org.jetbrains.exposed.dao.id.EntityID
import ru.iuturakulov.mybudgetbackend.entities.BaseIntEntity
import ru.iuturakulov.mybudgetbackend.entities.BaseIntEntityClass
import ru.iuturakulov.mybudgetbackend.entities.BaseIntIdTable

object UserProfileTable : BaseIntIdTable("users_profile") {
    val userId = reference("user_id", UserTable.id)
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val userDescription = text("user_description").nullable()
}

class UsersProfileEntity(id: EntityID<String>) : BaseIntEntity(id, UserProfileTable) {
    companion object : BaseIntEntityClass<UsersProfileEntity>(UserProfileTable)

    var userId by UserProfileTable.userId
    var firstName by UserProfileTable.firstName
    var lastName by UserProfileTable.lastName
    var userDescription by UserProfileTable.userDescription

    fun response() = UserProfile(
        userId.value,
        firstName,
        lastName,
        userDescription,
    )
}

data class UserProfile(
    var userId: String,
    val firstName: String?,
    val lastName: String?,
    val userDescription: String?,
)

