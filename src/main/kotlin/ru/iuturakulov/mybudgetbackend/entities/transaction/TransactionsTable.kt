package ru.iuturakulov.mybudgetbackend.entities.transaction

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.models.transaction.TransactionType

object TransactionsTable : Table("transactions") {
    val id = varchar("id", 36)
    val projectId = reference("project_id", ProjectsTable.id)
    val userId = reference("user_id", UserTable.id)
    val name = varchar("name", 255)
    val amount = decimal("amount", 12, 2)
    val category = varchar("category", 50)
    val categoryIcon = varchar("category_icon", 100)
    val date = long("date")
    val transactionType = enumerationByName("transaction_type", 10, TransactionType::class)
    val images = text("images")

    override val primaryKey = PrimaryKey(id)

    fun fromRow(row: ResultRow) = TransactionEntity(
        id = row[id],
        projectId = row[projectId],
        userId = row[userId],
        name = row[name],
        amount = row[amount].toDouble(),
        category = row[category],
        categoryIcon = row[categoryIcon],
        date = row[date],
        transactionType = row[transactionType],
        images = if (row[images].isBlank()) emptyList() else row[images].split(",")
    )
}
