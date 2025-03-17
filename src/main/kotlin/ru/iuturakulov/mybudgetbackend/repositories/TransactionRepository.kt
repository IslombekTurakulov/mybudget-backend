package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionEntity
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionsTable
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsFilter
import ru.iuturakulov.mybudgetbackend.models.transaction.TransactionType

class TransactionRepository {

    fun addTransaction(transaction: TransactionEntity): TransactionEntity = transaction {
        TransactionsTable.insert { insertStatement ->
            insertStatement[id] = transaction.id
            insertStatement[projectId] = transaction.projectId
            insertStatement[userId] = transaction.userId
            insertStatement[name] = transaction.name
            insertStatement[amount] = transaction.amount.toBigDecimal()
            transaction.category?.let {
                insertStatement[category] = transaction.category
            }
            transaction.categoryIcon?.let {
                insertStatement[categoryIcon] = transaction.categoryIcon
            }
            insertStatement[date] = transaction.date
            // Здесь вместо safe-call используем значение по умолчанию, если transactionType равен null
            insertStatement[transactionType] = transaction.transactionType ?: TransactionType.INCOME
            insertStatement[images] = transaction.images.orEmpty().joinToString(",")
        }
        transaction
    }

    fun getTransactionsByProject(projectId: String): List<TransactionEntity> = transaction {
        TransactionsTable.selectAll().where {
            TransactionsTable.projectId eq projectId
        }.map { TransactionsTable.fromRow(it) }
    }

    fun getTransactionById(transactionId: String): TransactionEntity? = transaction {
        TransactionsTable.selectAll().where { TransactionsTable.id eq transactionId }
            .mapNotNull { TransactionsTable.fromRow(it) }
            .singleOrNull()
    }

    fun updateTransaction(transaction: TransactionEntity): Boolean = transaction {
        TransactionsTable.update({ TransactionsTable.id eq transaction.id }) { statement ->
            statement[name] = transaction.name
            statement[amount] = transaction.amount.toBigDecimal()
            transaction.category?.let {
                statement[category] = transaction.category
            }
            transaction.categoryIcon?.let {
                statement[categoryIcon] = transaction.categoryIcon
            }
            statement[date] = transaction.date
            transaction.transactionType?.let {
                statement[transactionType] = transaction.transactionType
            }
            statement[images] = transaction.images.orEmpty().joinToString(",")
        } > 0
    }

    fun deleteTransaction(transactionId: String): Boolean = transaction {
        TransactionsTable.deleteWhere { id eq transactionId } > 0
    }

    fun getTransactionsByProject(projectId: String, filter: AnalyticsFilter?): List<TransactionEntity> = transaction {
        val query = TransactionsTable.selectAll().where { TransactionsTable.projectId eq projectId }

        // Фильтр по диапазону дат
        if (filter?.fromDate != null && filter.toDate != null) {
            query.andWhere { TransactionsTable.date.between(filter.fromDate, filter.toDate) }
        }

        // Фильтр по категориям
        if (filter?.categories?.isNotEmpty() == true) {
            query.andWhere { TransactionsTable.category inList filter.categories }
        }

        query.mapNotNull { TransactionsTable.fromRow(it) }
    }
}
