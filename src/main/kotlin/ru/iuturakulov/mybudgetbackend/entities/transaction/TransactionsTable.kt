package ru.iuturakulov.mybudgetbackend.entities.transaction

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.models.transaction.TransactionType

object TransactionsTable : Table("transactions") {
    val id = varchar("id", 36)
    val projectId = reference("project_id", ProjectsTable.id)
    val userId = reference("user_id", UserTable.id)  // только id пользователя
    val userName = varchar("user_name", 64) // просто строка, без ссылки
    val name = varchar("name", 255)
    val amount = decimal("amount", 12, 2)

    // Если в базе category может отсутствовать, объявляем nullable
    val category = varchar("category", 50).nullable()
    val categoryIcon = varchar("category_icon", 100).nullable()
    val date = long("date")
    val transactionType = enumerationByName("transaction_type", 10, TransactionType::class)

    // Сохраняем список изображений как строку с разделителем (например, запятая)
    val images = text("images")

    override val primaryKey = PrimaryKey(id)

    // Функция маппинга строки результата базы данных в TransactionEntity.
    fun fromRow(row: ResultRow, withProjectName: Boolean = false) = TransactionEntity(
        id = row[id],
        projectId = row[projectId],
        projectName = if (withProjectName) row[ProjectsTable.name] else null,
        userId = row[userId],
        userName = row[userName],
        name = row[name],
        amount = row[amount].toDouble(),
        category = row[category],
        categoryIcon = row[categoryIcon],
        date = row[date],
        type = row[transactionType],
        images = if (row[images].isBlank()) emptyList() else row[images].split(",")
    )
}
