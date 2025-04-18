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
import ru.iuturakulov.mybudgetbackend.models.transaction.AddTransactionRequest
import ru.iuturakulov.mybudgetbackend.models.transaction.TransactionType
import ru.iuturakulov.mybudgetbackend.models.transaction.UpdateTransactionRequest
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.TransactionRepository
import ru.iuturakulov.mybudgetbackend.services.NotificationService
import java.util.*

class TransactionController(
    private val transactionRepository: TransactionRepository,
    private val projectRepository: ProjectRepository,
    private val participantRepository: ParticipantRepository,
    private val accessControl: AccessControl,
    private val auditLog: AuditLogService,
    private val notificationService: NotificationService,
    private val clock: DateTimeProvider = SystemDateTimeProvider(),
) {
    private val limiter = RateLimiter(DEFAULT_SPAM_INTERVAL_SEC, clock)

    fun addTransaction(userId: String, req: AddTransactionRequest): TransactionEntity = transaction {
        val project = projectRepository.getProjectById(req.projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")
        val participant = participantRepository.getParticipantByUserAndProjectId(userId, req.projectId)
            ?: throw AppException.Authorization("Вы не участник проекта")
        if (!accessControl.canEditProject(userId, project, participant)) {
            throw AppException.Authorization("Нет прав для добавления транзакций")
        }

        // вычисляем дельты и проверяем бюджет
        val isExpense = req.type == TransactionType.EXPENSE
        val spentDelta = if (isExpense) req.amount else 0.0
        val budgetDelta = if (!isExpense) req.amount else 0.0
        val newSpent = project.amountSpent + spentDelta
        if (newSpent > project.budgetLimit) {
            throw AppException.InvalidProperty.Transaction("Сумма расходов превышает бюджет")
        }

        // обновляем проект за один вызов
        projectRepository.updateProjectAmounts(
            projectId   = project.id!!,
            amountSpent = newSpent.toBigDecimal(),
            budgetLimit = (project.budgetLimit + budgetDelta).toBigDecimal()
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
            images = req.images
        )
        transactionRepository.addTransaction(transactionEntitity)

        auditLog.logAction(userId, "Добавил транзакцию '${transactionEntitity.name}' в проект ${req.projectId}")
        notificationService.sendNotification(
            userId = project.ownerId,
            type = NotificationType.TRANSACTION_ADDED,
            message = "Пользователь $userId добавил транзакцию",
            projectId = req.projectId
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
        participantRepository.getParticipantByUserAndProjectId(userId, projectId)
            ?: throw AppException.Authorization("Вы не участник проекта")
        if (!accessControl.canViewProject(
                userId,
                project,
                participantRepository.getParticipantByUserAndProjectId(userId, projectId)!!
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
        participantRepository.getParticipantByUserAndProjectId(userId, oldTx.projectId)
            ?: throw AppException.Authorization("Вы не участник")
        if (!accessControl.canEditProject(
                userId,
                project,
                participantRepository.getParticipantByUserAndProjectId(userId, oldTx.projectId)!!
            )
        ) {
            throw AppException.Authorization("Нет прав на редактирование")
        }

        val oldExp = if (oldTx.type == TransactionType.EXPENSE) oldTx.amount else 0.0
        val newType = req.type ?: oldTx.type
        val newAmount = req.amount ?: oldTx.amount
        val newExpense = if (newType == TransactionType.EXPENSE) newAmount else 0.0
        val updatedSpent = project.amountSpent - oldExp + newExpense
        if (updatedSpent > project.budgetLimit) {
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
        project.id?.let { projectid ->
            projectRepository.updateProjectAmounts(projectId = projectid, amountSpent = updatedSpent.toBigDecimal())
        }

        auditLog.logAction(userId, "Обновил транзакцию '${updated.name}' в проекте ${oldTx.projectId}")
        updated
    }

    fun deleteTransaction(userId: String, txId: String) = transaction {
        val transactionEntity = transactionRepository.getTransactionById(txId)
            ?: throw AppException.NotFound.Transaction("Транзакция не найдена")
        val project = projectRepository.getProjectById(transactionEntity.projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")
        participantRepository.getParticipantByUserAndProjectId(userId, transactionEntity.projectId)
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
        val adjust = if (transactionEntity.type == TransactionType.EXPENSE) -transactionEntity.amount else 0.0
        project.id?.let { projectId ->
            projectRepository.updateProjectAmounts(
                projectId = projectId,
                amountSpent = (project.amountSpent + adjust).toBigDecimal()
            )
        }

        auditLog.logAction(userId, "Удалил транзакцию '${transactionEntity.name}' в проекте ${transactionEntity.projectId}")
        notificationService.sendNotification(
            userId = project.ownerId,
            type = NotificationType.TRANSACTION_REMOVED,
            message = "Пользователь $userId удалил транзакцию",
            projectId = transactionEntity.projectId
        )
    }
}