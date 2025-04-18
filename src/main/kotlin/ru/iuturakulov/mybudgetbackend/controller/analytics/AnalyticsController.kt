package ru.iuturakulov.mybudgetbackend.controller.analytics

import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsFilter
import ru.iuturakulov.mybudgetbackend.models.analytics.Granularity
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewAnalyticsDto
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewCategoryStats
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewPeriodStats
import ru.iuturakulov.mybudgetbackend.models.analytics.PeriodStats
import ru.iuturakulov.mybudgetbackend.models.analytics.ProjectAnalyticsDto
import ru.iuturakulov.mybudgetbackend.models.analytics.ProjectComparisonStats
import ru.iuturakulov.mybudgetbackend.repositories.AnalyticsRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.TransactionRepository
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

class AnalyticsController(
    private val analyticsRepository: AnalyticsRepository,
    private val projectRepository: ProjectRepository,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Общая аналитика по всем проектам пользователя
     */
    fun getOverviewAnalytics(
        userId: String,
        filter: AnalyticsFilter? = null
    ): OverviewAnalyticsDto {
        // Получаем все транзакции пользователя
        val allTxs = transactionRepository.getTransactionsByUser(userId)
        val txs = allTxs
            .filter { tx ->
                (filter?.fromDate?.let { tx.date >= it } ?: true)
                        && (filter?.toDate?.let { tx.date <= it } ?: true)
                        && (filter?.categories?.let { it.isEmpty() || it.contains(tx.category) } ?: true)
            }
            .takeIf { it.isNotEmpty() }
            ?: return OverviewAnalyticsDto(
                totalAmount = 0.0,
                categoryDistribution = emptyList(),
                periodDistribution = emptyList(),
                projectComparison = emptyList()
            )

        // Общая сумма
        val total = txs.sumOf { it.amount }

        // Распределение по категориям
        val byCategory = txs
            .groupBy { it.category ?: "Без категории" }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        // Распределение по периодам с учётом granularity
        val zone = ZoneId.systemDefault()
        val gran = filter?.granularity ?: Granularity.MONTH
        val byPeriod = txs
            .groupBy { tx ->
                val zdt = Instant.ofEpochMilli(tx.date).atZone(zone)
                when (gran) {
                    Granularity.DAY -> zdt.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    Granularity.WEEK -> {
                        val year = zdt.get(IsoFields.WEEK_BASED_YEAR)
                        val week = zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0')
                        "${year}-W${week}"
                    }

                    Granularity.MONTH -> YearMonth.from(zdt).toString()
                    Granularity.YEAR -> zdt.year.toString()
                }
            }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        // Сравнение по проектам
        val allProjects = projectRepository.getProjectsByUser(userId)
        val byProject = txs
            .groupBy { it.projectId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val categoryStats = byCategory.entries
            .map { (cat, amt) ->
                OverviewCategoryStats(
                    category = cat,
                    amount = amt,
                    percentage = if (total > 0) amt / total * 100 else 0.0
                )
            }
            .sortedByDescending { it.amount }

        val periodStats = byPeriod.entries
            .map { (period, amt) ->
                OverviewPeriodStats(
                    period = period,
                    amount = amt
                )
            }
            .sortedBy { it.period }

        val projectStats = allProjects
            .map { proj ->
                ProjectComparisonStats(
                    projectId = proj.id!!,
                    projectName = proj.name,
                    amount = byProject[proj.id] ?: 0.0
                )
            }
            .sortedByDescending { it.amount }

        return OverviewAnalyticsDto(
            totalAmount = total,
            categoryDistribution = categoryStats,
            periodDistribution = periodStats,
            projectComparison = projectStats
        )
    }

    /**
     * Более детальная аналитика по конкретному проекту
     */
    fun getProjectAnalytics(
        userId: String,
        projectId: String,
        filter: AnalyticsFilter? = null
    ): ProjectAnalyticsDto {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")
        if (!projectRepository.isUserParticipant(userId, projectId))
            throw AppException.Authorization("Вы не участник проекта")

        val txs = transactionRepository.getTransactionsByProject(projectId, filter)
            .takeIf { it.isNotEmpty() }
            ?: return ProjectAnalyticsDto(
                projectId = projectId,
                projectName = project.name,
                totalAmount = 0.0,
                categoryDistribution = emptyList(),
                periodDistribution = emptyList()
            )

        val total = txs.sumOf { it.amount }

        val byCategory = txs
            .groupBy { it.category ?: "Без категории" }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        // Группировка по периоду с учётом granularity
        val zone = ZoneId.systemDefault()
        val gran = filter?.granularity ?: Granularity.MONTH
        val byPeriod = txs.groupBy { tx ->
            val zdt = Instant.ofEpochMilli(tx.date).atZone(zone)
            when (gran) {
                Granularity.DAY -> zdt.format(DateTimeFormatter.ISO_LOCAL_DATE)
                Granularity.WEEK -> {
                    val week = zdt.get(IsoFields.WEEK_BASED_YEAR)
                    val wnum = zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0')
                    "$week-W$wnum"
                }

                Granularity.MONTH -> YearMonth.from(zdt).toString()
                Granularity.YEAR -> zdt.year.toString()
            }
        }.mapValues { (_, list) -> list.sumOf { it.amount } }

        val categoryStats = byCategory.entries
            .map { (cat, amt) -> OverviewCategoryStats(cat, amt, if (total > 0) amt / total * 100 else 0.0) }
            .sortedByDescending { it.amount }

        val periodStats = byPeriod.entries
            .map { (period, amt) -> PeriodStats(period = period, totalAmount = amt) }
            .sortedBy { it.period }

        return ProjectAnalyticsDto(
            projectId = projectId,
            projectName = project.name,
            totalAmount = total,
            categoryDistribution = categoryStats,
            periodDistribution = periodStats
        )
    }
}