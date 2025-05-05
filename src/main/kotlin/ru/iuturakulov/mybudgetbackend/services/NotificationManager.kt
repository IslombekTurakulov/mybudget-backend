package ru.iuturakulov.mybudgetbackend.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.not
import org.slf4j.LoggerFactory
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.fcm.NotificationContext
import ru.iuturakulov.mybudgetbackend.repositories.DeviceTokenRepository
import ru.iuturakulov.mybudgetbackend.utils.UTF8Control
import java.util.Locale
import java.util.ResourceBundle

class NotificationManager(
    private val notificationGuard: NotificationGuard,
    private val deviceRepo: DeviceTokenRepository,
    private val fcm: FcmService,
    private val overallNotificationService: OverallNotificationService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val logger = LoggerFactory.getLogger("NotificationManager")


    /**
     * Общий метод отправки уведомления с учётом локали.
     *
     * @param type Тип уведомления
     * @param ctx Набор данных для уведомления
     */
    fun sendNotification(
        type: NotificationType,
        ctx: NotificationContext,
        recipients: List<String>? = null
    ) {
        val userIds = recipients ?: determineRecipients(type, ctx.projectId)
        if (userIds.isEmpty()) return

        val tokenWithLanguageCodes = deviceRepo.findTokensAndLanguageCodeByUserIds(userIds)
        val params = type.toParams(ctx)

        tokenWithLanguageCodes.forEach { (token, languageCode, userId) ->
            scope.launch {
                try {
                    val isSelf = ctx.actorId != null && userId == ctx.actorId

                    val bundle = ResourceBundle.getBundle("messages", Locale(languageCode), UTF8Control())

                    val title = bundle.getString("${type.name}.title")
                    val bodyKey = when {
                        isSelf && bundle.containsKey("${type.name}.body.self") -> "${type.name}.body.self"
                        !isSelf && bundle.containsKey("${type.name}.body.actor") -> "${type.name}.body.actor"
                        else -> "${type.name}.body"
                    }

                    var body = bundle.getString(bodyKey)
                    params.forEach { (k, v) -> body = body.replace("{$k}", v) }

                    // Отправляем пуш только если это НЕ сам пользователь
                    if (!isSelf) {
                        fcm.send(
                            token = token,
                            title = title,
                            body = body,
                            type = type,
                            transactionId = ctx.transactionId,
                            senderId = null,
                            extra = params
                        )
                    }

                    // А вот в overralNotification — всегда
                    overallNotificationService.sendNotification(
                        userId = userId,
                        type = type,
                        message = "$title\n$body",
                    )

                } catch (t: Throwable) {
                    logger.error(t.localizedMessage) { "Failed to send notification of type $type" }
                }
            }
        }
    }

    /**
     * Логика выбора получателей по типу и проекту.
     * Сюда добавьте фильтр по глобальному флагу пользователя
     * и по `project_notification_prefs`.
     */
    private fun determineRecipients(
        type: NotificationType,
        projectId: String?
    ): List<String> {
        if (projectId == null) return emptyList()

        // получаем всех участников
        val participants = deviceRepo.findParticipantIds(projectId)
        return notificationGuard.filterReceivers(
            candidateUserIds = participants,
            type = type,
            projectId = projectId
        )
    }

    fun NotificationType.toParams(ctx: NotificationContext): Map<String, String> {
        val result = mutableMapOf<String, String>()

        ctx.projectId?.let { result["projectId"] = it }
        ctx.projectName?.let { result["projectName"] = it }
        ctx.actor?.let { result["actor"] = it }
        ctx.transactionId?.let { result["txId"] = it }
        ctx.transactionName?.let { result["transactionName"] = it }
        ctx.details?.let { result["details"] = it }
        ctx.systemMessage?.let { result["message"] = it }

        fun processExtraParams() {
            when (this) {
                NotificationType.TRANSACTION_ADDED,
                NotificationType.TRANSACTION_UPDATED -> {
                    val spentBefore = ctx.beforeSpent ?: return
                    val spentAfter = ctx.afterSpent ?: return
                    val limit = ctx.budgetLimit ?: return
                    result["amount"] = budgetLine(spentBefore, spentAfter, limit)
                }

                NotificationType.BUDGET_THRESHOLD -> {
                    val spentAfter = ctx.afterSpent ?: return
                    val limit = ctx.budgetLimit ?: return
                    val percent = if (limit == 0.0) 0.0 else spentAfter / limit * 100
                    result["percent"] = "%.1f".format(percent)
                }

                else -> Unit
            }
        }

        processExtraParams()

        return result
    }

    private fun money(v: Double) = "%,.2f ₽".format(v)
    private fun pct(v: Double) = "%.1f%%".format(v)

    private fun budgetLine(spentBefore: Double, spentAfter: Double, limit: Double): String {
        val pb = if (limit == 0.0) 0.0 else (spentBefore / limit * 100)
        val pa = if (limit == 0.0) 0.0 else (spentAfter / limit * 100)
        return "${money(spentBefore)} (${pct(pb)}) → ${money(spentAfter)} (${pct(pa)})"
    }
}