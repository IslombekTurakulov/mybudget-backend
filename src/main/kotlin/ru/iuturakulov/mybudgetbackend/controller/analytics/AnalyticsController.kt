package ru.iuturakulov.mybudgetbackend.controller.analytics

import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsFilter
import ru.iuturakulov.mybudgetbackend.models.analytics.CategoryStats
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewAnalyticsDto
import ru.iuturakulov.mybudgetbackend.models.analytics.PeriodStats
import ru.iuturakulov.mybudgetbackend.models.analytics.ProjectAnalyticsDto
import ru.iuturakulov.mybudgetbackend.repositories.AnalyticsRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.TransactionRepository
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsController(
    private val analyticsRepository: AnalyticsRepository,
    private val projectRepository: ProjectRepository,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Получить общую аналитику пользователя
     */
    fun getOverviewAnalytics(userId: String): OverviewAnalyticsDto {
        return analyticsRepository.getOverviewAnalytics(userId)
    }


    fun getProjectAnalytics(userId: String, projectId: String, filter: AnalyticsFilter?): ProjectAnalyticsDto {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        if (!projectRepository.isUserParticipant(userId, projectId)) {
            throw AppException.Authorization("Вы не участник проекта")
        }

        val transactions = transactionRepository.getTransactionsByProject(projectId, filter)

        // Подсчет общей суммы расходов
        val totalSpent = transactions.sumOf { transactionEntity -> transactionEntity.amount }

        // Распределение по категориям (фильтрованные категории)
        val categoryDistribution = transactions
            .groupBy { transactionEntity -> transactionEntity.category }
            .mapNotNull { (category, txList) ->
                category?.let {
                    CategoryStats(
                        category = category,
                        totalAmount = txList.sumOf { it.amount }
                    )
                }
            }

        // Распределение по периодам (группируем по месяцам)
        val periodDistribution = transactions.groupBy { tx ->
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(tx.date))
        }.map { (period, txList) ->
            PeriodStats(
                period = period,
                totalAmount = txList.sumOf { transactionEntity -> transactionEntity.amount }
            )
        }

        return ProjectAnalyticsDto(
            projectId = projectId,
            projectName = project.name,
            totalAmount = totalSpent,
            categoryDistribution = categoryDistribution,
            periodDistribution = periodDistribution
        )
    }
}
