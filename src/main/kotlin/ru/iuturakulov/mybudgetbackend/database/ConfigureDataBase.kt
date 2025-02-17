package ru.iuturakulov.mybudgetbackend.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import javax.sql.DataSource

fun configureDatabase() {
    DatabaseConfig.initDB()
    transaction {
        addLogger(StdOutSqlLogger)
        create(UserTable)
    }
}

object DatabaseConfig {
    fun initDB() {
        val config = HikariConfig("/hikari.properties")
        val dataSource = HikariDataSource(config)
        runFlyway(dataSource)
        Database.connect(dataSource)
    }

    private fun runFlyway(dataSource: DataSource) {
        try {
            val flyway = Flyway.configure().dataSource(dataSource).load()
            flyway.migrate()
        } catch (e: FlywayException) {
            println("⚠ Ошибка миграции Flyway: ${e.message}")
            throw e
        }
    }
}
