package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionsTable
import ru.iuturakulov.mybudgetbackend.models.analytics.CategoryStats
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewAnalyticsDto
import ru.iuturakulov.mybudgetbackend.models.analytics.PeriodStats
import ru.iuturakulov.mybudgetbackend.models.analytics.ProjectComparison
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsRepository {

    /**
     * Получить общую аналитику пользователя (все проекты)
     */
    fun getOverviewAnalytics(userId: String): OverviewAnalyticsDto = transaction {
        val projects = ProjectsTable.selectAll()
            .where { ProjectsTable.ownerId eq userId }
            .mapNotNull { ProjectsTable.fromRow(it) }

        val transactions = projects.flatMap { project ->
            TransactionsTable.selectAll().where { TransactionsTable.projectId.eq(project.id.orEmpty()) }
                .mapNotNull { TransactionsTable.fromRow(it) }
        }

        val totalSpent = transactions.sumOf { it.amount }

        val categoryStats = transactions.groupBy { it.category }
            .mapNotNull { (category, txList) ->
                category?.let {
                    CategoryStats(category, txList.sumOf { it.amount })
                }
            }

        val periodStats = transactions.groupBy {
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date))
        }.map { (period, txList) ->
            PeriodStats(period, txList.sumOf { it.amount })
        }

        val projectComparison = projects.mapNotNull { project ->
            val spent = transactions.filter { it.projectId == project.id }.sumOf { it.amount }
            project.id?.let { ProjectComparison(it, project.name, spent) }
        }

        OverviewAnalyticsDto(
            totalSpent = totalSpent,
            categoryDistribution = categoryStats,
            periodDistribution = periodStats,
            projectComparison = projectComparison
        )
    }
}
