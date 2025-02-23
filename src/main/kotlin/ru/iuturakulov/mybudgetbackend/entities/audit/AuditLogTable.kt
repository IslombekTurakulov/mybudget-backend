package ru.iuturakulov.mybudgetbackend.entities.audit

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import java.util.*

object AuditLogTable : Table("audit_logs") {
    val id = varchar("id", 36).default(UUID.randomUUID().toString())
    val userId = reference("user_id", UserTable.id, onDelete = ReferenceOption.CASCADE)
    val action = text("action")
    val timestamp = long("timestamp").default(System.currentTimeMillis())

    override val primaryKey = PrimaryKey(id)

    fun fromRow(row: ResultRow) = AuditLogEntity(
        id = row[id],
        userId = row[userId],
        action = row[action],
        timestamp = row[timestamp]
    )
}
