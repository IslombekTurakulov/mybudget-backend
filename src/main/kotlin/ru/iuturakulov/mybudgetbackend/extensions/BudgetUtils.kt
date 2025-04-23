package ru.iuturakulov.mybudgetbackend.extensions

import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.services.NotificationService

object BudgetUtils {

    /**
     * Проверяем долю расходов: если >= 90 %, отправляем
     * владельцу уведомление о приближении к лимиту.
     */
    fun maybeNotifyLimit(
        project: ProjectEntity,
        amountSpent: Double,
        participantRepo: ParticipantRepository,
        notificationService: NotificationService
    ) {
        val limit = project.budgetLimit
        if (limit == 0.0) return                     // лимит ещё не задан

        val usedPct = amountSpent / limit * 100.0
        if (usedPct < 90.0) return                  // всё в пределах нормы

        val owner = participantRepo.getProjectOwnerId(project.id!!)
            ?: return                                // на всякий случай

        val msg = "Расходы проекта «${project.name}» " +
                "достигли ${"%.1f".format(usedPct)} % бюджета " +
                "(потрачено ${amountSpent.formatMoney()} из " +
                "${limit.formatMoney()})"

        notificationService.sendNotification(
            userId = owner.userId,
            type = NotificationType.BUDGET_THRESHOLD,
            message = msg,
            projectId = project.id
        )
    }

    private fun Double.formatMoney() = "%,.2f ₽".format(this)
}
