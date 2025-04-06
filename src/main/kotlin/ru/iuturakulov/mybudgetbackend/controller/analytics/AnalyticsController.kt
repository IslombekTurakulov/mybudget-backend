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


    fun getProjectAnalytics(
        userId: String,
        projectId: String,
        filter: AnalyticsFilter?
    ): ProjectAnalyticsDto {
        // 1. Валидация проекта и прав доступа
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        if (!projectRepository.isUserParticipant(userId, projectId)) {
            throw AppException.Authorization("Вы не участник проекта")
        }

        // 2. Получение транзакций с учетом фильтра

        val transactions = transactionRepository.getTransactionsByProject(projectId, filter)
            .takeIf { it.isNotEmpty() }
            ?: return ProjectAnalyticsDto(
                projectId = projectId,
                projectName = project.name,
                totalAmount = 0.0,
                categoryDistribution = emptyList(),
                periodDistribution = emptyList()
            )

        // 3. Оптимизированный расчет статистики
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val (categoryStats, periodStats) = transactions
            .asSequence()
            .fold(
                Triple(
                    mutableMapOf<String, Double>(), // category -> sum
                    mutableMapOf<String, Double>(), // period -> sum
                    0.0 // total
                )
            ) { (categoryMap, periodMap, total), tx ->
                // Категории
                tx.category?.let { category ->
                    categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + tx.amount
                }

                // Периоды
                val period = dateFormat.format(Date(tx.date))
                periodMap[period] = periodMap.getOrDefault(period, 0.0) + tx.amount

                Triple(categoryMap, periodMap, total + tx.amount)
            }

        // 4. Сортировка результатов
        val sortedCategories = categoryStats
            .map { (category, amount) -> CategoryStats(category, amount) }
            .sortedByDescending { it.totalAmount }

        val sortedPeriods = periodStats
            .map { (period, amount) -> PeriodStats(period, amount) }
            .sortedBy { it.period } // Сортировка по дате

        val totalSpent = project.amountSpent

        return ProjectAnalyticsDto(
            projectId = projectId,
            projectName = project.name,
            totalAmount = totalSpent,
            categoryDistribution = sortedCategories,
            periodDistribution = sortedPeriods
        )
    }
}
