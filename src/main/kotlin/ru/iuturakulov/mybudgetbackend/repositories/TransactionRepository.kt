package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SortOrder
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

    fun addTransaction(tx: TransactionEntity): TransactionEntity = transaction {
        TransactionsTable.insert { stmt ->
            stmt[id] = tx.id
            stmt[projectId] = tx.projectId
            stmt[userId] = tx.userId
            stmt[userName] = tx.userName
            stmt[name] = tx.name
            stmt[amount] = tx.amount.toBigDecimal()

            tx.category?.let { stmt[category] = it }
            tx.categoryIcon?.let { stmt[categoryIcon] = it }

            stmt[date] = tx.date
            // если type == null, ставим EXPENSE по-умолчанию
            stmt[transactionType] = tx.type ?: TransactionType.EXPENSE

            stmt[images] = tx.images
                .orEmpty()
                .joinToString(",")
        }
        tx
    }

    fun getTransactionById(transactionId: String): TransactionEntity? = transaction {
        TransactionsTable
            .selectAll().where { TransactionsTable.id eq transactionId }
            .limit(1)
            .firstOrNull()
            ?.let(TransactionsTable::fromRow)
    }

    fun getTransactionsByProject(projectId: String): List<TransactionEntity> = transaction {
        TransactionsTable
            .selectAll().where { TransactionsTable.projectId eq projectId }
            .orderBy(TransactionsTable.date to SortOrder.ASC)
            .map(TransactionsTable::fromRow)
    }

    fun getTransactionsByUser(userId: String): List<TransactionEntity> = transaction {
        TransactionsTable
            .selectAll().where { TransactionsTable.userId eq userId }
            .orderBy(TransactionsTable.date to SortOrder.ASC)
            .map(TransactionsTable::fromRow)
    }

    fun updateTransaction(tx: TransactionEntity): Boolean = transaction {
        TransactionsTable.update({ TransactionsTable.id eq tx.id }) { stmt ->
            stmt[name] = tx.name
            stmt[amount] = tx.amount.toBigDecimal()

            tx.category?.let { stmt[category] = it }
            tx.categoryIcon?.let { stmt[categoryIcon] = it }

            stmt[date] = tx.date
            // обновляем только если type != null
            tx.type?.let { stmt[transactionType] = it }

            stmt[images] = tx.images
                .orEmpty()
                .joinToString(",")
        } > 0
    }

    fun deleteTransaction(transactionId: String): Boolean = transaction {
        TransactionsTable.deleteWhere { TransactionsTable.id eq transactionId } > 0
    }

    fun getTransactionsByProject(
        projectId: String,
        filter: AnalyticsFilter?
    ): List<TransactionEntity> = transaction {
        // базовый запрос
        var transactions = TransactionsTable
            .selectAll().where { TransactionsTable.projectId eq projectId }

        // диапазон дат
        filter?.fromDate
            ?.let { from -> filter.toDate?.let { to ->
                transactions = transactions.andWhere { TransactionsTable.date.between(from, to) }
            } }

        // категории
        filter?.categories
            ?.map { if (it == "Без категории" || it == "No category") "" else it }
            .takeIf { it?.isNotEmpty() == true }
            ?.let { cats ->
                transactions = transactions.andWhere { TransactionsTable.category inList cats!! }
            }

        transactions.orderBy(TransactionsTable.date to SortOrder.ASC)
            .map(TransactionsTable::fromRow)
    }
}