package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.user.EmailVerificationTable

class EmailVerificationRepository {

    fun saveVerificationCode(email: String, code: String) = transaction {
        // удалить старую запись, если есть
        EmailVerificationTable.deleteWhere { EmailVerificationTable.email eq email }

        EmailVerificationTable.insert {
            it[EmailVerificationTable.email] = email
            it[EmailVerificationTable.code] = code
            it[createdAt] = System.currentTimeMillis()
        }
    }

    fun verifyCode(email: String, code: String): Boolean = transaction {
        val result = EmailVerificationTable
            .selectAll().where { EmailVerificationTable.email eq email }
            .singleOrNull() ?: return@transaction false

        val storedCode = result[EmailVerificationTable.code]
        storedCode == code
    }

    fun removeVerificationCode(email: String) = transaction {
        EmailVerificationTable.deleteWhere { EmailVerificationTable.email eq email }
    }
}
