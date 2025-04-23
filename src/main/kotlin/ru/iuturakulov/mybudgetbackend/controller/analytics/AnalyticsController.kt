package ru.iuturakulov.mybudgetbackend.controller.analytics

import io.ktor.http.ContentType
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsExportFormat
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsFilter
import ru.iuturakulov.mybudgetbackend.models.analytics.ExportFile
import ru.iuturakulov.mybudgetbackend.models.analytics.Granularity
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewAnalyticsDto
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewCategoryStats
import ru.iuturakulov.mybudgetbackend.models.analytics.OverviewPeriodStats
import ru.iuturakulov.mybudgetbackend.models.analytics.PeriodStats
import ru.iuturakulov.mybudgetbackend.models.analytics.ProjectAnalyticsDto
import ru.iuturakulov.mybudgetbackend.models.analytics.ProjectComparisonStats
import ru.iuturakulov.mybudgetbackend.models.analytics.TransactionInfo
import ru.iuturakulov.mybudgetbackend.models.analytics.UserStats
import ru.iuturakulov.mybudgetbackend.repositories.AnalyticsRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.TransactionRepository
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import ru.iuturakulov.mybudgetbackend.services.EmailService
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

class AnalyticsController(
    private val analyticsRepository: AnalyticsRepository,
    private val projectRepository: ProjectRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
) {

    /**
     * Общая аналитика по всем проектам пользователя
     */
    fun getOverviewAnalytics(
        userId: String,
        filter: AnalyticsFilter? = null
    ): OverviewAnalyticsDto {
        // Получаем все транзакции пользователя и сразу фильтруем
        val txs = transactionRepository.getTransactionsByUser(userId)
            .filter { tx ->
                (filter?.fromDate?.let { tx.date >= it } ?: true) &&
                        (filter?.toDate?.let { tx.date <= it } ?: true) &&
                        (filter?.categories?.let { it.isEmpty() || it.contains(tx.category) } ?: true)
            }
            .takeIf { it.isNotEmpty() }
            ?: return OverviewAnalyticsDto(
                totalAmount = 0.0,
                totalCount = 0,
                averageAmount = 0.0,
                minAmount = 0.0,
                maxAmount = 0.0,
                categoryDistribution = emptyList(),
                periodDistribution = emptyList(),
                projectComparison = emptyList()
            )

        // Общая статистика
        val totalCount = txs.size
        val total = txs.sumOf { it.amount }
        val averageAmount = total / totalCount
        val minAmount = txs.minOf { it.amount }
        val maxAmount = txs.maxOf { it.amount }

        // Распределение по категориям
        val categoryStats = txs
            .groupBy { it.category.takeIf { it?.isNotEmpty() == true } ?: "Без категории" }
            .map { (cat, list) ->
                val amt = list.sumOf { it.amount }
                val cnt = list.size
                val txInfos = list.map { tx ->
                    TransactionInfo(
                        id = tx.id,
                        name = tx.name,
                        amount = tx.amount,
                        date = Instant.ofEpochMilli(tx.date)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE),
                        userName = tx.userName,
                        type = tx.type.name,
                        categoryIcon = tx.categoryIcon
                    )
                }
                OverviewCategoryStats(
                    category = cat,
                    amount = amt,
                    percentage = if (total > 0) amt / total * 100 else 0.0,
                    count = cnt,
                    transactionInfo = txInfos,
                )
            }
            .sortedByDescending { it.amount }

        // Распределение по периодам
        val zone = ZoneId.systemDefault()
        val gran = filter?.granularity ?: Granularity.MONTH
        val periodStats = txs
            .groupBy { tx ->
                val zdt = Instant.ofEpochMilli(tx.date).atZone(zone)
                when (gran) {
                    Granularity.DAY -> zdt.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    Granularity.WEEK -> "${zdt.get(IsoFields.WEEK_BASED_YEAR)}-W${
                        zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0')
                    }"

                    Granularity.MONTH -> YearMonth.from(zdt).toString()
                    Granularity.YEAR -> zdt.year.toString()
                }
            }
            .map { (period, list) ->
                val amt = list.sumOf { it.amount }
                val cnt = list.size
                OverviewPeriodStats(
                    period = period,
                    amount = amt,
                    count = cnt
                )
            }
            .sortedBy { it.period }

        // Сравнение по проектам
        val allProjects = projectRepository.getProjectsByUser(userId)
        val projectStats = txs
            .groupBy { it.projectId }
            .mapNotNull { (projId, list) ->
                val proj = allProjects.find { it.id == projId } ?: return@mapNotNull null
                val amt = list.sumOf { it.amount }
                val cnt = list.size
                ProjectComparisonStats(
                    projectId = proj.id!!,
                    projectName = proj.name,
                    amount = amt,
                    count = cnt
                )
            }
            .sortedByDescending { it.amount }

        return OverviewAnalyticsDto(
            totalAmount = total,
            totalCount = totalCount,
            averageAmount = averageAmount,
            minAmount = minAmount,
            maxAmount = maxAmount,
            categoryDistribution = categoryStats,
            periodDistribution = periodStats,
            projectComparison = projectStats
        )
    }


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
                totalCount = 0,
                averageAmount = 0.0,
                minAmount = 0.0,
                maxAmount = 0.0,
                amountSpent = 0.0,
                budgetLimit = 0.0,
                categoryDistribution = emptyList(),
                periodDistribution = emptyList(),
                userDistribution = emptyList()
            )

        // Общая статистика
        val totalCount = txs.size
        val total = txs.sumOf { it.amount }
        val averageAmount = total / totalCount
        val minAmount = txs.minOf { it.amount }
        val maxAmount = txs.maxOf { it.amount }

        // Распределение по категориям
        val categoryStats = txs
            .groupBy { it.category.takeIf { it?.isNotEmpty() == true } ?: "Без категории" }
            .map { (cat, list) ->
                val amt = list.sumOf { it.amount }
                val cnt = list.size
                val txInfos = list.map { tx ->
                    TransactionInfo(
                        id = tx.id,
                        name = tx.name,
                        amount = tx.amount,
                        date = Instant.ofEpochMilli(tx.date)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE),
                        userName = tx.userName,
                        type = tx.type.name,
                        categoryIcon = tx.categoryIcon
                    )
                }
                OverviewCategoryStats(
                    category = cat,
                    amount = amt,
                    percentage = if (total > 0) amt / total * 100 else 0.0,
                    count = cnt,
                    transactionInfo = txInfos
                )
            }
            .sortedByDescending { it.amount }

        // Распределение по периодам
        val zone = ZoneId.systemDefault()
        val gran = filter?.granularity ?: Granularity.MONTH
        val periodStats = txs
            .groupBy { tx ->
                val zdt = Instant.ofEpochMilli(tx.date).atZone(zone)
                when (gran) {
                    Granularity.DAY -> zdt.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    Granularity.WEEK -> "${zdt.get(IsoFields.WEEK_BASED_YEAR)}-W${
                        zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0')
                    }"

                    Granularity.MONTH -> YearMonth.from(zdt).toString()
                    Granularity.YEAR -> zdt.year.toString()
                }
            }
            .map { (period, list) ->
                val amt = list.sumOf { it.amount }
                val cnt = list.size
                PeriodStats(
                    period = period,
                    totalAmount = amt,
                    count = cnt
                )
            }
            .sortedBy { it.period }

        // Распределение по пользователям
        val userStats = txs
            .groupBy { it.userId }
            .map { (uid, list) ->
                val amt = list.sumOf { it.amount }
                val cnt = list.size
                val name = userRepository.getUserById(uid)?.name ?: uid
                UserStats(
                    userId = uid,
                    userName = name,
                    amount = amt,
                    count = cnt
                )
            }
            .sortedByDescending { it.amount }

        return ProjectAnalyticsDto(
            projectId = projectId,
            projectName = project.name,
            totalAmount = total,
            totalCount = totalCount,
            budgetLimit = project.budgetLimit,
            amountSpent = project.amountSpent,
            averageAmount = averageAmount,
            minAmount = minAmount,
            maxAmount = maxAmount,
            categoryDistribution = categoryStats,
            periodDistribution = periodStats,
            userDistribution = userStats
        )
    }

    /**
     * Экспорт аналитики (CSV или PDF) c учётом фильтров.
     * Если projectId == null — выгружаем «Обзор по всем проектам».
     */
    fun exportAnalytics(
        userId: String,
        projectId: String?,
        filter: AnalyticsFilter?,
        format: AnalyticsExportFormat
    ): ExportFile {
        val user = userRepository.getUserById(userId)
            ?: throw AppException.NotFound.User("Пользователь не найден")

        val project = projectId?.let { projectRepository.getProjectById(it) }

        val analytics = if (projectId == null)
            getOverviewAnalytics(userId, filter)
        else
            getProjectAnalytics(userId, projectId, filter)

        val bytes: ByteArray
        val contentType: ContentType
        val fileName: String

        when (format) {
            AnalyticsExportFormat.CSV -> {
                bytes = AnalyticsExporter.toCsv(analytics)
                contentType = ContentType.Text.CSV
                fileName = "analytics_${projectId ?: "overview"}_${filter?.toSuffix()}.csv"
            }

            AnalyticsExportFormat.PDF -> {
                bytes = AnalyticsExporter.toPdf(analytics)
                contentType = ContentType.Application.Pdf
                fileName = "analytics_${projectId ?: "overview"}_${filter?.toSuffix()}.pdf"
            }
        }

        emailService.sendAnalyticsExportEmail(
            topicName = project?.let { "проекта \"${project.name}\"" } ?: "\"Общая\"",
            toEmail = user.email,
            attachmentName = fileName,
            attachmentBytes = bytes,
            exportFormat = format
        )

        return ExportFile(contentType, fileName, bytes)
    }
}