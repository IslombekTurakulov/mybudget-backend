package ru.iuturakulov.mybudgetbackend.controller.analytics

import com.itextpdf.io.font.FontProgramFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.opencsv.CSVWriter
import com.opencsv.CSVWriterBuilder
import com.opencsv.ICSVWriter
import com.opencsv.ICSVWriter.DEFAULT_ESCAPE_CHARACTER
import com.opencsv.ICSVWriter.DEFAULT_QUOTE_CHARACTER
import ru.iuturakulov.mybudgetbackend.models.analytics.*
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Конвертация DTO аналитики в PDF / CSV.
 *
 * Поддерживаются два варианта DTO:
 *  * [ru.iuturakulov.mybudgetbackend.models.analytics.OverviewAnalyticsDto]– «Сводка по всем проектам»
 *  * [ru.iuturakulov.mybudgetbackend.models.analytics.ProjectAnalyticsDto] – аналитика внутри одного проекта
 *
 */
object AnalyticsExporter {

    /** Экспорт в CSV */
    fun toCsv(dto: Any): ByteArray {
        val sw = StringWriter()
        val csv = CSVWriterBuilder(sw)
            .withSeparator(';')
            .withQuoteChar(DEFAULT_QUOTE_CHARACTER)
            .withEscapeChar(DEFAULT_ESCAPE_CHARACTER)
            .withLineEnd(System.lineSeparator())
            .build()

        val now = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        csv.use {
            // Заголовок
            it.writeNext(arrayOf("# Отчёт аналитики"))
            it.writeNext(arrayOf("# Дата экспорта", now))
            it.writeNext(emptyArray())

            process(
                dto,
                onOverview = { o -> writeOverviewCsv(o, it) },
                onProject = { p -> writeProjectCsv(p, it) }
            )
        }

        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        return bom + sw.toString().toByteArray(Charsets.UTF_8)
    }

    private fun writeOverviewCsv(o: OverviewAnalyticsDto, csv: ICSVWriter) {
        // Сводная аналитика
        csv.writeNext(arrayOf("# Сводная аналитика"))
        listOf(
            "Общая сумма" to money(o.totalAmount),
            "Кол‑во транзакций" to o.totalCount.toString(),
            "Средняя сумма" to money(o.averageAmount),
            "Мин. сумма" to money(o.minAmount),
            "Макс. сумма" to money(o.maxAmount)
        ).forEach { csv.writeNext(arrayOf(it.first, it.second)) }
        csv.writeNext(emptyArray())

        // Детализация по категориям
        o.categoryDistribution.forEach { cd ->
            csv.writeNext(arrayOf("## Категория: ${cd.category}", "Сумма: ${money(cd.amount)}", "Доля: ${pct(cd.percentage)}%", "Кол‑во: ${cd.count}"))
            csv.writeNext(arrayOf("ID","Название","Дата","Пользователь","Тип","Сумма"))
            cd.transactionInfo.forEach { tx ->
                csv.writeNext(arrayOf(
                    tx.id,
                    tx.name,
                    tx.date,
                    tx.userName,
                    tx.type,
                    money(tx.amount)
                ))
            }
            csv.writeNext(emptyArray())
        }

        writeSection(
            title = "# Распределение по категориям",
            headers = arrayOf("Категория", "Сумма", "Доля, %", "Кол‑во"),
            rows = o.categoryDistribution,
            csv = csv
        ) { cd -> arrayOf(cd.category, money(cd.amount), pct(cd.percentage), cd.count.toString()) }

        writeSection(
            title = "# Распределение по периодам",
            headers = arrayOf("Период", "Сумма", "Кол‑во"),
            rows = o.periodDistribution,
            csv = csv
        ) { pd -> arrayOf(pd.period, money(pd.amount), pd.count.toString()) }

        writeSection(
            title = "# Сравнение проектов",
            headers = arrayOf("ID", "Название", "Сумма", "Кол‑во"),
            rows = o.projectComparison,
            csv = csv
        ) { pc -> arrayOf(pc.projectId, pc.projectName, money(pc.amount), pc.count.toString()) }
    }

    private fun writeProjectCsv(p: ProjectAnalyticsDto, csv: ICSVWriter) {
        csv.writeNext(arrayOf("# Аналитика проекта «${p.projectName}»"))
        csv.writeNext(emptyArray())

        listOf(
            "Бюджет проекта" to money(p.budgetLimit),
            "Потрачено всего" to money(p.amountSpent),
            "Общая сумма транзакций" to money(p.totalAmount),
            "Кол‑во транзакций" to p.totalCount.toString(),
            "Средняя сумма" to money(p.averageAmount),
            "Мин. сумма" to money(p.minAmount),
            "Макс. сумма" to money(p.maxAmount)
        ).forEach { csv.writeNext(arrayOf(it.first, it.second)) }
        csv.writeNext(emptyArray())

        // Детализация по категориям
        p.categoryDistribution.forEach { cd ->
            csv.writeNext(arrayOf("## Категория: ${cd.category}", "Сумма: ${money(cd.amount)}", "Доля: ${pct(cd.percentage)}%", "Кол‑во: ${cd.count}"))
            csv.writeNext(arrayOf("ID","Название","Дата","Пользователь","Тип","Сумма"))
            cd.transactionInfo.forEach { tx ->
                csv.writeNext(arrayOf(
                    tx.id,
                    tx.name,
                    tx.date,
                    tx.userName,
                    tx.type,
                    money(tx.amount)
                ))
            }
            csv.writeNext(emptyArray())
        }

        writeSection(
            title = "# Распределение по категориям",
            headers = arrayOf("Категория", "Сумма", "Доля, %", "Кол‑во"),
            rows = p.categoryDistribution,
            csv = csv
        ) { cd -> arrayOf(cd.category, money(cd.amount), pct(cd.percentage), cd.count.toString()) }

        writeSection(
            title = "# Распределение по периодам",
            headers = arrayOf("Период", "Сумма", "Кол‑во"),
            rows = p.periodDistribution,
            csv = csv
        ) { pd -> arrayOf(pd.period, money(pd.totalAmount), pd.count.toString()) }

        writeSection(
            title = "# Распределение по пользователям",
            headers = arrayOf("Пользователь", "Сумма", "Кол‑во"),
            rows = p.userDistribution,
            csv = csv
        ) { us -> arrayOf(us.userName, money(us.amount), us.count.toString()) }
    }

    private fun <T> writeSection(
        title: String,
        headers: Array<String>,
        rows: List<T>,
        csv: ICSVWriter,
        mapRow: (T) -> Array<String>
    ) {
        if (rows.isEmpty()) return
        csv.writeNext(arrayOf(title))
        csv.writeNext(headers)
        rows.forEach { csv.writeNext(mapRow(it)) }
        csv.writeNext(emptyArray())
    }

    /** Экспорт в PDF */
    fun toPdf(dto: Any): ByteArray {
        val baos = ByteArrayOutputStream()
        val pdf = PdfDocument(PdfWriter(baos))
        val doc = Document(pdf).apply { setMargins(36f, 36f, 36f, 36f) }

        // Встраиваем шрифт с кириллицей
        val fontProgram = FontProgramFactory.createFont("/fonts/NotoSans-Medium.ttf")
        val font = PdfFontFactory.createFont(
            fontProgram,
            PdfEncodings.IDENTITY_H,
            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
        )
        doc.setFont(font).setFontSize(10f)

        // Шапка отчёта как таблица
        val now = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val headerTbl = Table(UnitValue.createPercentArray(floatArrayOf(2f, 3f)))
            .useAllAvailableWidth()
            .apply {
                addHeaderCell("Отчёт аналитики", colspan = 2, align = TextAlignment.CENTER)
                addCell("Дата экспорта:")
                addCell(now)
                setMarginBottom(12f)
            }
        doc.add(headerTbl)

        process(
            dto,
            onOverview = { addOverviewPdf(it, doc) },
            onProject = { addProjectPdf(it, doc) }
        )

        doc.close()
        return baos.toByteArray()
    }

    private fun addOverviewPdf(o: OverviewAnalyticsDto, doc: Document) {
        doc.add(sectionTitle("Сводная аналитика"))

        Table(UnitValue.createPercentArray(floatArrayOf(3f,2f,2f,2f,2f))).useAllAvailableWidth().apply {
            addHeaderCell("Всего",1); addHeaderCell("Транз.",1);
            addHeaderCell("Сред.",1); addHeaderCell("Мин.",1); addHeaderCell("Макс.",1)
            listOf(
                money(o.totalAmount), o.totalCount.toString(),
                money(o.averageAmount), money(o.minAmount), money(o.maxAmount)
            ).forEach { addTextCell(it,TextAlignment.RIGHT) }
            setMarginBottom(12f)
        }.let { doc.add(it) }

        // metrics summary
        val summary = Table(UnitValue.createPercentArray(floatArrayOf(3f, 2f, 2f, 2f, 2f)))
            .useAllAvailableWidth()
            .apply {
                addHeaderCell("Всего", align = TextAlignment.CENTER)
                addHeaderCell("Транз.", align = TextAlignment.CENTER)
                addHeaderCell("Сред.", align = TextAlignment.CENTER)
                addHeaderCell("Мин.", align = TextAlignment.CENTER)
                addHeaderCell("Макс.", align = TextAlignment.CENTER)

                addTextCell(money(o.totalAmount), align = TextAlignment.RIGHT)
                addTextCell(o.totalCount.toString(), align = TextAlignment.RIGHT)
                addTextCell(money(o.averageAmount), align = TextAlignment.RIGHT)
                addTextCell(money(o.minAmount), align = TextAlignment.RIGHT)
                addTextCell(money(o.maxAmount), align = TextAlignment.RIGHT)

                setMarginBottom(12f)
            }
        doc.add(summary)

        doc.add(subSectionTitle("Распределение по категориям"))
        doc.add(tableCategories(o.categoryDistribution))

        o.categoryDistribution.forEach { cd ->
            doc.add(subSectionTitle("Категория: ${cd.category} (сумма ${money(cd.amount)})"))
            Table(UnitValue.createPercentArray(floatArrayOf(3f,3f,2f,2f,2f,2f))).useAllAvailableWidth().apply {
                headerRow("ID","Название","Дата","Пользователь","Тип","Сумма")
                cd.transactionInfo.forEach { tx ->
                    addCell(tx.id); addCell(tx.name); addCell(tx.date);
                    addCell(tx.userName); addCell(tx.type); addTextCell(money(tx.amount),TextAlignment.RIGHT)
                }
                setMarginBottom(12f)
            }.let { doc.add(it) }
        }

        doc.add(subSectionTitle("Распределение по периодам"))
        doc.add(tablePeriods(o.periodDistribution))

        doc.add(subSectionTitle("Сравнение проектов"))
        doc.add(tableProjects(o.projectComparison))
    }

    private fun addProjectPdf(p: ProjectAnalyticsDto, doc: Document) {
        doc.add(sectionTitle("Аналитика проекта «${p.projectName}»"))

        // Бюджет vs Потрачено
        val bal = Table(UnitValue.createPercentArray(floatArrayOf(3f, 3f)))
            .useAllAvailableWidth()
            .apply {
                addHeaderCell("Бюджет", align = TextAlignment.CENTER)
                addHeaderCell("Потрачено", align = TextAlignment.CENTER)
                addTextCell(money(p.budgetLimit), align = TextAlignment.RIGHT)
                addTextCell(money(p.amountSpent), align = TextAlignment.RIGHT)
                setMarginBottom(12f)
            }
        doc.add(bal)

        val summary = Table(UnitValue.createPercentArray(floatArrayOf(3f, 2f, 2f, 2f, 2f)))
            .useAllAvailableWidth()
            .apply {
                addHeaderCell("Всего", align = TextAlignment.CENTER)
                addHeaderCell("Транз.", align = TextAlignment.CENTER)
                addHeaderCell("Сред.", align = TextAlignment.CENTER)
                addHeaderCell("Мин.", align = TextAlignment.CENTER)
                addHeaderCell("Макс.", align = TextAlignment.CENTER)

                addTextCell(money(p.totalAmount), align = TextAlignment.RIGHT)
                addTextCell(p.totalCount.toString(), align = TextAlignment.RIGHT)
                addTextCell(money(p.averageAmount), align = TextAlignment.RIGHT)
                addTextCell(money(p.minAmount), align = TextAlignment.RIGHT)
                addTextCell(money(p.maxAmount), align = TextAlignment.RIGHT)

                setMarginBottom(12f)
            }
        doc.add(summary)

        doc.add(subSectionTitle("Распределение по категориям"))
        doc.add(tableCategories(p.categoryDistribution))

        p.categoryDistribution.forEach { cd ->
            doc.add(subSectionTitle("Категория: ${cd.category} (сумма ${money(cd.amount)})"))
            Table(UnitValue.createPercentArray(floatArrayOf(3f,3f,2f,2f,2f,2f))).useAllAvailableWidth().apply {
                headerRow("ID","Название","Дата","Пользователь","Тип","Сумма")
                cd.transactionInfo.forEach { tx ->
                    addCell(tx.id); addCell(tx.name); addCell(tx.date);
                    addCell(tx.userName); addCell(tx.type); addTextCell(money(tx.amount),TextAlignment.RIGHT)
                }
                setMarginBottom(12f)
            }.let { doc.add(it) }
        }

        doc.add(subSectionTitle("Распределение по периодам"))
        doc.add(tablePeriodsProject(p.periodDistribution))

        doc.add(subSectionTitle("Распределение по пользователям"))
        doc.add(tableUsers(p.userDistribution))
    }

    private fun Table.addHeaderCell(text: String, colspan: Int = 1, align: TextAlignment = TextAlignment.LEFT) {
        addCell(
            Cell(1, colspan)
                .add(Paragraph(text).setBold())
                .setTextAlignment(align)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        )
    }

    private fun sectionTitle(text: String) =
        Paragraph(text).setFontSize(14f).setBold().setMarginTop(12f).setMarginBottom(6f)

    private fun subSectionTitle(text: String) =
        Paragraph(text).setFontSize(12f).setBold().setMarginTop(8f).setMarginBottom(4f)

    // Таблицы категорий, периодов, проектов, пользователей
    private fun tableCategories(rows: List<OverviewCategoryStats>): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(4f, 2f, 2f, 1f))).useAllAvailableWidth().apply {
            headerRow("Категория", "Сумма", "Доля, %", "Кол‑во")
            rows.forEach {
                addCell(it.category)
                addTextCell(money(it.amount), TextAlignment.RIGHT)
                addTextCell(pct(it.percentage), TextAlignment.RIGHT)
                addTextCell(it.count.toString(), TextAlignment.RIGHT)
            }
        }

    private fun tablePeriods(rows: List<OverviewPeriodStats>): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(4f, 2f, 1f))).useAllAvailableWidth().apply {
            headerRow("Период", "Сумма", "Кол‑во")
            rows.forEach {
                addCell(it.period)
                addTextCell(money(it.amount), TextAlignment.RIGHT)
                addTextCell(it.count.toString(), TextAlignment.RIGHT)
            }
        }

    private fun tablePeriodsProject(rows: List<PeriodStats>): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(4f, 2f, 1f))).useAllAvailableWidth().apply {
            headerRow("Период", "Сумма", "Кол‑во")
            rows.forEach {
                addCell(it.period)
                addTextCell(money(it.totalAmount), TextAlignment.RIGHT)
                addTextCell(it.count.toString(), TextAlignment.RIGHT)
            }
        }

    private fun tableProjects(rows: List<ProjectComparisonStats>): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(3f, 4f, 2f, 1f))).useAllAvailableWidth().apply {
            headerRow("ID", "Название", "Сумма", "Кол‑во")
            rows.forEach {
                addCell(it.projectId)
                addCell(it.projectName)
                addTextCell(money(it.amount), TextAlignment.RIGHT)
                addTextCell(it.count.toString(), TextAlignment.RIGHT)
            }
        }

    private fun tableUsers(rows: List<UserStats>): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(4f, 2f, 1f))).useAllAvailableWidth().apply {
            headerRow("Пользователь", "Сумма", "Кол‑во")
            rows.forEach {
                addCell(it.userName)
                addTextCell(money(it.amount), TextAlignment.RIGHT)
                addTextCell(it.count.toString(), TextAlignment.RIGHT)
            }
        }

    private fun Table.headerRow(vararg titles: String) {
        titles.forEach { addCell(header(it)) }
    }

    private fun header(text: String) =
        Cell().add(Paragraph(text).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setTextAlignment(TextAlignment.CENTER)

    private fun money(v: Double) = DecimalFormat("#,##0.## ₽").format(v)
    private fun pct(v: Double) = DecimalFormat("#0.##").format(v)

    private inline fun process(
        dto: Any,
        onOverview: (OverviewAnalyticsDto) -> Unit,
        onProject: (ProjectAnalyticsDto) -> Unit
    ) = when (dto) {
        is OverviewAnalyticsDto -> onOverview(dto)
        is ProjectAnalyticsDto -> onProject(dto)
        else -> error("Unknown type: ${dto::class.simpleName}")
    }

    private fun Table.addTextCell(
        text: String,
        align: TextAlignment = TextAlignment.LEFT
    ) {
        this.addCell(
            Cell()
                .add(Paragraph(text))
                .setTextAlignment(align)
        )
    }
}
