package ru.iuturakulov.mybudgetbackend.models.project

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import ru.iuturakulov.mybudgetbackend.models.UserRole

data class InviteParticipantRequest(
    val email: String?,
    val type: InvitationType,
    val role: UserRole
) {

    enum class InvitationType {
        QR,
        MANUAL;
    }

    fun validation() {
        validate(this) {
            if (type == InvitationType.MANUAL) {
                validate(InviteParticipantRequest::email).isNotBlank().isEmail()
            }
            validate(InviteParticipantRequest::role).isNotNull()
        }
    }
}
