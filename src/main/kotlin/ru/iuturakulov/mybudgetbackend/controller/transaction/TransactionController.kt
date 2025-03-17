package ru.iuturakulov.mybudgetbackend.controller.transaction

import org.jetbrains.exposed.sql.transactions.transaction
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
import services.NotificationService
import java.util.*

class TransactionController(
    private val transactionRepository: TransactionRepository,
    private val projectRepository: ProjectRepository,
    private val participantRepository: ParticipantRepository,
    private val accessControl: AccessControl,
    private val auditLogService: AuditLogService,
    private val notificationService: NotificationService
) {

    fun addTransaction(userId: String, request: AddTransactionRequest): TransactionEntity = transaction {
        val project = projectRepository.getProjectById(request.projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        val participant = participantRepository.getParticipantByUserAndProjectId(
            userId = userId,
            projectId = request.projectId
        ) ?: throw AppException.Authorization("Вы не участник проекта")

        if (!accessControl.canEditProject(userId, project, participant)) {
            throw AppException.Authorization("Вы не можете добавлять транзакции в этот проект")
        }

        // Если транзакция является расходом, то увеличиваем сумму расходов,
        // если доходом – сумма расходов остаётся неизменной.
        val isExpense = request.transactionType == TransactionType.EXPENSE
        val newSpent = if (isExpense) project.amountSpent + request.amount else project.amountSpent

        if (isExpense && newSpent > project.budgetLimit) {
            throw AppException.InvalidProperty.Transaction("Сумма расходов превышает бюджет проекта")
        }

        val transaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            projectId = request.projectId,
            userId = userId,
            name = request.name,
            amount = request.amount,
            category = request.category,
            categoryIcon = request.categoryIcon,
            date = System.currentTimeMillis(),
            transactionType = request.transactionType ?: TransactionType.INCOME,
            images = request.images
        )

        transactionRepository.addTransaction(transaction)

        // Обновляем сумму расходов только если транзакция – расход
        projectRepository.updateAmountSpent(request.projectId, newSpent)

        auditLogService.logAction(userId, "Добавил транзакцию: ${transaction.name} в проект ${request.projectId}")

        notificationService.sendNotification(
            userId = project.ownerId,
            type = NotificationType.TRANSACTION_ADDED,
            message = "Пользователь $userId добавил новую транзакцию в проект ${request.projectId}",
            projectId = request.projectId
        )

        return@transaction transaction
    }

    fun getProjectTransactions(userId: String, projectId: String): List<TransactionEntity> {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        if (!projectRepository.isUserParticipant(userId, projectId)) {
            throw AppException.Authorization("Вы не участник проекта")
        }

        return transactionRepository.getTransactionsByProject(projectId)
    }

    fun getTransactionById(userId: String, projectId: String, transactionId: String): TransactionEntity {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
            ?: throw AppException.Authorization("Вы не участник проекта")

        if (!accessControl.canViewProject(userId, project, participant)) {
            throw AppException.Authorization("У вас нет прав на просмотр транзакции")
        }

        return transactionRepository.getTransactionById(transactionId)
            ?: throw AppException.NotFound.Transaction("Транзакция не найдена")
    }

    fun updateTransaction(userId: String, request: UpdateTransactionRequest): TransactionEntity = transaction {
        val transaction = transactionRepository.getTransactionById(request.transactionId)
            ?: throw AppException.NotFound.Transaction("Транзакция не найдена")

        val project = projectRepository.getProjectById(transaction.projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        project.id ?: throw AppException.NotFound.Project("Проект не найден")

        val participant = participantRepository.getParticipantByUserAndProjectId(
            userId = userId,
            projectId = transaction.projectId
        ) ?: throw AppException.Authorization("Вы не участник проекта")

        if (!accessControl.canEditProject(userId, project, participant)) {
            throw AppException.Authorization("Вы не можете редактировать транзакции в этом проекте")
        }

        // Сначала "убираем" вклад старой транзакции, если она была расходом
        val oldExpense = if (transaction.transactionType == TransactionType.EXPENSE) transaction.amount else 0.0

        // Новые значения: если поле не передано, остаётся старое значение
        val newType = request.transactionType ?: transaction.transactionType
        val newAmount = request.amount ?: transaction.amount
        // Вклад новой транзакции, если она расход
        val newExpense = if (newType == TransactionType.EXPENSE) newAmount else 0.0

        // Пересчитываем сумму расходов проекта
        val updatedAmountSpent = project.amountSpent - oldExpense + newExpense

        if (updatedAmountSpent > project.budgetLimit) {
            throw AppException.InvalidProperty.Transaction("Сумма расходов превышает бюджет проекта")
        }

        val updatedTransaction = transaction.copy(
            name = request.name ?: transaction.name,
            amount = newAmount,
            category = request.category ?: transaction.category,
            categoryIcon = request.categoryIcon ?: transaction.categoryIcon,
            date = request.date ?: transaction.date,
            transactionType = newType,
            images = request.images ?: transaction.images
        )

        transactionRepository.updateTransaction(updatedTransaction)
        projectRepository.updateAmountSpent(project.id, updatedAmountSpent)

        auditLogService.logAction(userId, "Обновил транзакцию: ${request.name ?: transaction.name} в проекте ${transaction.projectId}")

        return@transaction updatedTransaction
    }

    fun deleteTransaction(userId: String, transactionId: String) = transaction {
        val transaction = transactionRepository.getTransactionById(transactionId)
            ?: throw AppException.NotFound.Transaction("Транзакция не найдена")

        val project = projectRepository.getProjectById(transaction.projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        project.id ?: throw AppException.NotFound.Project("Проект не найден")

        val participant = participantRepository.getParticipantByUserAndProjectId(
            userId = userId,
            projectId = transaction.projectId
        ) ?: throw AppException.Authorization("Вы не участник проекта")

        if (!accessControl.canEditProject(userId, project, participant)) {
            throw AppException.Authorization("Вы не можете удалять транзакции в этом проекте")
        }

        transactionRepository.deleteTransaction(transactionId)

        // Если удаляемая транзакция – расход, вычитаем её сумму
        val newSpent = if (transaction.transactionType == TransactionType.EXPENSE) {
            project.amountSpent - transaction.amount
        } else {
            project.amountSpent
        }

        projectRepository.updateAmountSpent(project.id, newSpent)

        auditLogService.logAction(
            userId,
            "Удалил транзакцию: ${transaction.name} в проекте ${transaction.projectId}"
        )

        notificationService.sendNotification(
            userId = project.ownerId,
            type = NotificationType.TRANSACTION_REMOVED,
            message = "Пользователь $userId удалил транзакцию из проекта ${transaction.projectId}",
            projectId = transaction.projectId
        )
    }
}
