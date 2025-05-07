package ru.iuturakulov.mybudgetbackend.controller.transaction

import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.controller.user.DEFAULT_SPAM_INTERVAL_SEC
import ru.iuturakulov.mybudgetbackend.controller.user.DateTimeProvider
import ru.iuturakulov.mybudgetbackend.controller.user.RateLimiter
import ru.iuturakulov.mybudgetbackend.controller.user.SystemDateTimeProvider
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionEntity
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.models.fcm.NotificationContext
import ru.iuturakulov.mybudgetbackend.models.transaction.AddTransactionRequest
import ru.iuturakulov.mybudgetbackend.models.transaction.TransactionType
import ru.iuturakulov.mybudgetbackend.models.transaction.UpdateTransactionRequest
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.TransactionRepository
import ru.iuturakulov.mybudgetbackend.services.NotificationManager
import java.util.*

class TransactionController(
    private val transactionRepository: TransactionRepository,
    private val projectRepository: ProjectRepository,
    private val participantRepository: ParticipantRepository,
    private val accessControl: AccessControl,
    private val auditLog: AuditLogService,
    private val clock: DateTimeProvider = SystemDateTimeProvider(),
    private val notificationManager: NotificationManager
) {
    private val limiter = RateLimiter(DEFAULT_SPAM_INTERVAL_SEC, clock)

    fun addTransaction(userId: String, req: AddTransactionRequest): TransactionEntity =
        transaction {
            val project = projectRepository.getProjectById(req.projectId)
                ?: throw AppException.NotFound.Project("Проект не найден")
            val participant = participantRepository.getParticipantByUserAndProjectId(userId, req.projectId)
                ?: throw AppException.Authorization("Вы не участник проекта")
            if (!accessControl.canEditProject(userId, project, participant)) {
                throw AppException.Authorization("Нет прав для добавления транзакций")
            }

            // вычисляем дельты и проверяем бюджет
            val beforeSpent = project.amountSpent
            val isExpense = req.type == TransactionType.EXPENSE
            val spentDelta = if (isExpense) req.amount else 0.0
            val limitDelta = if (!isExpense) req.amount else 0.0
            val afterSpent = project.amountSpent + spentDelta

            if (afterSpent > project.budgetLimit) {
                throw AppException.InvalidProperty.Transaction("Сумма расходов превышает бюджет")
            }

            // обновляем проект за один вызов
            projectRepository.updateProjectAmounts(
                projectId = project.id,
                amountSpent = (project.amountSpent + spentDelta).toBigDecimal(),
                budgetLimit = (project.budgetLimit + limitDelta).toBigDecimal()
            )

            val nowEpoch = clock.now().toEpochMilli()
            val transactionEntitity = TransactionEntity(
                id = UUID.randomUUID().toString(),
                projectId = req.projectId,
                userId = participant.userId,
                userName = participant.name,
                name = req.name,
                amount = req.amount,
                category = req.category,
                categoryIcon = req.categoryIcon,
                date = nowEpoch,
                type = req.type ?: TransactionType.INCOME,
                images = req.images,
                projectName = project.name
            )
            transactionRepository.addTransaction(transactionEntitity)

            auditLog.logAction(userId, "Добавил транзакцию '${transactionEntitity.name}' в проект ${req.projectId}")
            notificationManager.sendNotification(
                type = NotificationType.TRANSACTION_ADDED,
                ctx = NotificationContext(
                    actor = participant.name,
                    actorId = participant.userId,
                    projectId = project.id,
                    projectName = project.name,
                    transactionId = transactionEntitity.id,
                    transactionName = transactionEntitity.name,
                    beforeSpent = beforeSpent,
                    afterSpent = afterSpent,
                    budgetLimit = project.budgetLimit
                )
            )
            transactionEntitity
        }

    fun getProjectTransactions(userId: String, projectId: String): List<TransactionEntity> {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")
        if (!projectRepository.isUserParticipant(userId, projectId)) {
            throw AppException.Authorization("Вы не участник проекта")
        }
        return transactionRepository.getTransactionsByProject(projectId)
            .sortedBy { it.date }
            .map { tx -> if (tx.userId == userId) tx.copy(userName = "Вы") else tx }
    }

    fun getTransactionById(userId: String, projectId: String, txId: String): TransactionEntity {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")
        val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
            ?: throw AppException.Authorization("Вы не участник проекта")
        if (!accessControl.canViewProject(
                userId,
                project,
                participant
            )
        ) {
            throw AppException.Authorization("Нет прав для просмотра")
        }
        return transactionRepository.getTransactionById(txId)
            ?: throw AppException.NotFound.Transaction("Транзакция не найдена")
    }

    fun updateTransaction(userId: String, req: UpdateTransactionRequest): TransactionEntity = transaction {
        val oldTx = transactionRepository.getTransactionById(req.transactionId)
            ?: throw AppException.NotFound.Transaction("Не найдена транзакция")
        val project = projectRepository.getProjectById(oldTx.projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")
        val participant = participantRepository.getParticipantByUserAndProjectId(userId, oldTx.projectId)
            ?: throw AppException.Authorization("Вы не участник")
        if (!accessControl.canEditProject(
                userId,
                project,
                participantRepository.getParticipantByUserAndProjectId(userId, oldTx.projectId)!!
            )
        ) {
            throw AppException.Authorization("Нет прав на редактирование")
        }

        val newType = req.type ?: oldTx.type
        val newAmount = req.amount ?: oldTx.amount

        val wasExpense = oldTx.type == TransactionType.EXPENSE
        val willExpense = newType == TransactionType.EXPENSE
        val beforeSpent = project.amountSpent

        val spentDelta = when {
            wasExpense && willExpense -> newAmount - oldTx.amount
            wasExpense && !willExpense -> -oldTx.amount
            !wasExpense && willExpense -> newAmount
            else -> 0.0
        }

        val limitDelta = when {
            wasExpense && willExpense -> 0.0
            wasExpense && !willExpense -> newAmount
            !wasExpense && willExpense -> -oldTx.amount
            else -> newAmount - oldTx.amount
        }
        val afterSpent = project.amountSpent + spentDelta
        val newLimit = project.budgetLimit + limitDelta

        if (afterSpent > newLimit) {
            throw AppException.InvalidProperty.Transaction("Сумма расходов превышает бюджет")
        }

        val updated = oldTx.copy(
            name = req.name ?: oldTx.name,
            amount = newAmount,
            category = req.category ?: oldTx.category,
            categoryIcon = req.categoryIcon ?: oldTx.categoryIcon,
            date = req.date ?: oldTx.date,
            type = newType,
            images = req.images
        )

        transactionRepository.updateTransaction(updated)
        projectRepository.updateProjectAmounts(
            projectId = project.id!!,
            amountSpent = afterSpent.toBigDecimal(),
            budgetLimit = newLimit.toBigDecimal()
        )

        auditLog.logAction(userId, "Обновил транзакцию '${updated.name}' в проекте ${oldTx.projectId}")

        notificationManager.sendNotification(
            type = NotificationType.TRANSACTION_UPDATED,
            ctx = NotificationContext(
                actor = participant.name,
                actorId = participant.userId,
                projectId = project.id,
                projectName = project.name,
                transactionId = updated.id,
                transactionName = updated.name,
                beforeSpent = beforeSpent,
                afterSpent = afterSpent,
                budgetLimit = project.budgetLimit
            )
        )
        updated
    }

    fun deleteTransaction(userId: String, txId: String) = transaction {
        val transactionEntity = transactionRepository.getTransactionById(txId)
            ?: throw AppException.NotFound.Transaction("Транзакция не найдена")
        val project = projectRepository.getProjectById(transactionEntity.projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")
        val participant = participantRepository.getParticipantByUserAndProjectId(userId, transactionEntity.projectId)
            ?: throw AppException.Authorization("Вы не участник проекта")
        if (!accessControl.canEditProject(
                userId,
                project,
                participantRepository.getParticipantByUserAndProjectId(userId, transactionEntity.projectId)!!
            )
        ) {
            throw AppException.Authorization("Нет прав на удаление")
        }

        transactionRepository.deleteTransaction(txId)
        val spentDelta = if (transactionEntity.type == TransactionType.EXPENSE)
            -transactionEntity.amount else 0.0
        val limitDelta = if (transactionEntity.type == TransactionType.INCOME)
            -transactionEntity.amount else 0.0

        val afterSpent = project.amountSpent + spentDelta
        val newLimit = project.budgetLimit + limitDelta
        projectRepository.updateProjectAmounts(
            projectId = project.id!!,
            amountSpent = afterSpent.toBigDecimal(),
            budgetLimit = newLimit.toBigDecimal()
        )

        auditLog.logAction(
            userId,
            "Удалил транзакцию '${transactionEntity.name}' в проекте ${transactionEntity.projectId}"
        )

        notificationManager.sendNotification(
            type = NotificationType.TRANSACTION_REMOVED,
            ctx = NotificationContext(
                actor = participant.name,
                actorId = participant.userId,
                projectId = project.id,
                projectName = project.name,
                transactionId = transactionEntity.id,
                transactionName = transactionEntity.name,
                beforeSpent = project.amountSpent,
                afterSpent = afterSpent,
                budgetLimit = project.budgetLimit
            )
        )
    }
}