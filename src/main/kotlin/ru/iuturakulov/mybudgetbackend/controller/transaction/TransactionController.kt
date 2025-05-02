package ru.iuturakulov.mybudgetbackend.controller.transaction

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.controller.user.DEFAULT_SPAM_INTERVAL_SEC
import ru.iuturakulov.mybudgetbackend.controller.user.DateTimeProvider
import ru.iuturakulov.mybudgetbackend.controller.user.RateLimiter
import ru.iuturakulov.mybudgetbackend.controller.user.SystemDateTimeProvider
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionEntity
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.extensions.BudgetUtils.maybeNotifyLimit
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
        val beforeSpent = project.amountSpent
        val isExpense = req.type == TransactionType.EXPENSE
        val spentDelta = if (isExpense) req.amount else 0.0
        val limitDelta = if (!isExpense) req.amount else 0.0
        val newSpent = project.amountSpent + spentDelta

        if (newSpent > project.budgetLimit) {
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
            images = req.images
        )
        transactionRepository.addTransaction(transactionEntitity)

        auditLog.logAction(userId, "Добавил транзакцию '${transactionEntitity.name}' в проект ${req.projectId}")
        val participants = ParticipantTable.selectAll().where { ParticipantTable.projectId eq project.id.orEmpty() }
            .mapNotNull { it[ParticipantTable.userId] }

        notifyAll(
            participants = participants,
            actorId = userId,
            actorName = participant.name,
            actorEmail = participant.email,
            txName = transactionEntitity.name,
            txAmount = transactionEntitity.amount,
            action = "добавил",
            project = project,
            beforeSpent = beforeSpent,
            afterSpent = newSpent
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
        val newSpent = project.amountSpent + spentDelta
        val newLimit = project.budgetLimit + limitDelta

        if (newSpent > newLimit) {
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
            amountSpent = newSpent.toBigDecimal(),
            budgetLimit = newLimit.toBigDecimal()
        )

        maybeNotifyLimit(
            project = project,
            amountSpent = newSpent,
            participantRepo = participantRepository,
            notificationService = notificationService
        )

        auditLog.logAction(userId, "Обновил транзакцию '${updated.name}' в проекте ${oldTx.projectId}")
        val participants = ParticipantTable.selectAll().where { ParticipantTable.projectId eq project.id.orEmpty() }
            .mapNotNull { it[ParticipantTable.userId] }

        notifyAll(
            participants,
            actorId = userId,
            actorName = participant.name,
            actorEmail = participant.email,
            txName = updated.name,
            txAmount = updated.amount,
            action = "обновил",
            project = project,
            beforeSpent = beforeSpent,
            afterSpent = newSpent
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

        val newSpent = project.amountSpent + spentDelta
        val newLimit = project.budgetLimit + limitDelta
        projectRepository.updateProjectAmounts(
            projectId = project.id!!,
            amountSpent = newSpent.toBigDecimal(),
            budgetLimit = newLimit.toBigDecimal()
        )

        maybeNotifyLimit(
            project = project,
            amountSpent = newSpent,
            participantRepo = participantRepository,
            notificationService = notificationService
        )

        auditLog.logAction(
            userId,
            "Удалил транзакцию '${transactionEntity.name}' в проекте ${transactionEntity.projectId}"
        )
        val participants = ParticipantTable.selectAll().where { ParticipantTable.projectId eq project.id.orEmpty() }
            .mapNotNull { it[ParticipantTable.userId] }

        notifyAll(
            participants,
            actorId = userId,
            actorName = participant.name,
            actorEmail = participant.email,
            txName = transactionEntity.name,
            txAmount = transactionEntity.amount,
            action = "удалил",
            project = project,
            beforeSpent = project.amountSpent,
            afterSpent = newSpent
        )
    }

    private fun money(v: Double) = "%,.2f ₽".format(v)
    private fun pct(v: Double) = "%.1f%%".format(v)

    private fun budgetLine(spentBefore: Double, spentAfter: Double, limit: Double): String {
        val pb = if (limit == 0.0) 0.0 else (spentBefore / limit * 100)
        val pa = if (limit == 0.0) 0.0 else (spentAfter / limit * 100)
        return "${money(spentBefore)} (${pct(pb)}) -> ${money(spentAfter)} (${pct(pa)})"
    }

    private fun notifyAll(
        participants: List<String>,
        actorId: String,
        actorName: String,
        actorEmail: String,
        txName: String,
        txAmount: Double,
        action: String,             // «добавил», «обновил», «удалил»
        project: ProjectEntity,
        beforeSpent: Double,
        afterSpent: Double
    ) {
        val limit = project.budgetLimit
        val remaining = limit - afterSpent
        val projectInfo = "Проект «${project.name}» (лимит ${money(limit)})"

        participants.forEach { uid ->
            val header = if (uid == actorId) {
                "Вы ${action}и транзакцию"
            } else {
                "Пользователь $actorName ($actorEmail) $action транзакцию"
            }

            val body = listOf(
                "\"$txName\" — ${money(txAmount)}",
                "Расходы: ${budgetLine(beforeSpent, afterSpent, limit)}",
                "Осталось: ${money(remaining)}"
            ).joinToString(separator = "\n")

            notificationService.sendNotification(
                userId = uid,
                type = when (action) {
                    "добавил" -> NotificationType.TRANSACTION_ADDED
                    "обновил" -> NotificationType.TRANSACTION_UPDATED
                    else -> NotificationType.TRANSACTION_REMOVED
                },
                message = listOf(projectInfo, header, body).joinToString("\n"),
                projectId = project.id
            )
        }
    }

}