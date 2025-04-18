package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionsTable
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewAnalyticsDto
import ru.iuturakulov.mybudgetbackend.models.analytics.PeriodStats
import ru.iuturakulov.mybudgetbackend.models.analytics.ProjectComparison
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsRepository {

    /**
     * Получает агрегированную аналитику по всем проектам пользователя
     *
     * @param userId ID пользователя для фильтрации проектов
     * @return DTO с аналитикой по категориям, периодам и проектам
     */
//    fun getOverviewAnalytics(userId: String): OverviewAnalyticsDto = transaction {
//        // Фильтруем проекты где он является овнером
//        val projects = ProjectsTable.selectAll()
//            .where { ProjectsTable.ownerId eq userId }
//            .mapNotNull { ProjectsTable.fromRow(it) }
//
//        val transactions = projects.flatMap { project ->
//            TransactionsTable.selectAll().where { TransactionsTable.projectId.eq(project.id.orEmpty()) }
//                .mapNotNull { TransactionsTable.fromRow(it) }
//            }
//
//        if (transactions.isEmpty()) {
//            return@transaction OverviewAnalyticsDto(
//                totalSpent = 0.0,
//                categoryDistribution = emptyList(),
//                periodDistribution = emptyList(),
//                projectComparison = emptyList()
//            )
//        }
//
//        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
//
//        val totalSpent = transactions.sumOf { it.amount }
//
//        val categoryStats = transactions
//            .asSequence()
//            .filter { it.category != null }
//            .groupBy { it.category!! }
//            .map { (category, txList) ->
//                CategoryStats(
//                    category = category,
//                    totalAmount = txList.sumOf { it.amount }
//                )
//            }
//            .sortedByDescending { it.totalAmount }
//
//        val periodStats = transactions
//            .groupBy { dateFormat.format(Date(it.date)) }
//            .map { (period, txList) ->
//                PeriodStats(
//                    period = period,
//                    totalAmount = txList.sumOf { it.amount }
//                )
//            }
//            .sortedBy { it.period }
//
//        val projectComparison = projects.mapNotNull { project ->
//            project.id?.let { pid ->
//                val spent = transactions
//                    .filter { it.projectId == pid }
//                    .sumOf { it.amount }
//
//                ProjectComparison(
//                    projectId = pid,
//                    projectName = project.name,
//                    totalSpent = spent
//                )
//            }
//        }.sortedByDescending { it.totalSpent }
//
//        OverviewAnalyticsDto(
//            totalSpent = totalSpent,
//            categoryDistribution = categoryStats,
//            periodDistribution = periodStats,
//            projectComparison = projectComparison
//        )
//    }
}
