package ru.iuturakulov.mybudgetbackend.controller.transaction

import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionEntity
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.models.transaction.AddTransactionRequest
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

    fun addTransaction(userId: String, request: AddTransactionRequest): TransactionEntity {
        return transaction {
            val project = projectRepository.getProjectById(request.projectId)
                ?: throw AppException.NotFound.Project("Проект не найден")

            val participant = participantRepository.getParticipantByUserAndProjectId(
                userId = userId,
                projectId = request.projectId
            ) ?: throw AppException.Authorization("Вы не участник проекта")

            if (!accessControl.canEditProject(userId, project, participant)) {
                throw AppException.Authorization("Вы не можете добавлять транзакции в этот проект")
            }

            // Проверка превышения бюджета
            val totalSpent = transactionRepository.getTransactionsByProject(request.projectId)
                .sumOf { it.amount }
            if (totalSpent + request.amount > project.budgetLimit) {
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
                date = System.currentTimeMillis()
            )

            transactionRepository.addTransaction(transaction)

            auditLogService.logAction(userId, "Добавил транзакцию: ${transaction.name} в проект ${request.projectId}")

            notificationService.sendNotification(
                userId = project.ownerId,
                type = NotificationType.TRANSACTION_ADDED,
                message = "Пользователь $userId добавил новую транзакцию в проект ${request.projectId}",
                projectId = request.projectId
            )

            return@transaction transaction
        }
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

    fun updateTransaction(userId: String, request: UpdateTransactionRequest): TransactionEntity {
        return transaction {
            val transaction = transactionRepository.getTransactionById(request.transactionId)
                ?: throw AppException.NotFound.Transaction("Транзакция не найдена")

            val project = projectRepository.getProjectById(transaction.projectId)
                ?: throw AppException.NotFound.Project("Проект не найден")

            val participant = participantRepository.getParticipantByUserAndProjectId(
                userId = userId,
                projectId = transaction.projectId
            ) ?: throw AppException.Authorization("Вы не участник проекта")

            if (!accessControl.canEditProject(userId, project, participant)) {
                throw AppException.Authorization("Вы не можете редактировать транзакции в этом проекте")
            }

            auditLogService.logAction(userId, "Обновил транзакцию: ${request.name} в проекте ${transaction.projectId}")

            val transactionEntity = transaction.copy(
                name = request.name ?: transaction.name,
                amount = request.amount ?: transaction.amount,
                category = request.category ?: transaction.category,
                categoryIcon = request.categoryIcon ?: transaction.categoryIcon,
                date = request.date ?: transaction.date
            )
            transactionRepository.updateTransaction(transactionEntity)
            transactionEntity
        }
    }

    fun deleteTransaction(userId: String, transactionId: String) {
        return transaction {
            val transaction = transactionRepository.getTransactionById(transactionId)
                ?: throw AppException.NotFound.Transaction("Транзакция не найдена")

            val project = projectRepository.getProjectById(transaction.projectId)
                ?: throw AppException.NotFound.Project("Проект не найден")

            val participant = participantRepository.getParticipantByUserAndProjectId(
                userId = userId,
                projectId = transaction.projectId
            )
                ?: throw AppException.Authorization("Вы не участник проекта")

            if (!accessControl.canEditProject(userId, project, participant)) {
                throw AppException.Authorization("Вы не можете удалять транзакции в этом проекте")
            }

            transactionRepository.deleteTransaction(transactionId)

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
}
