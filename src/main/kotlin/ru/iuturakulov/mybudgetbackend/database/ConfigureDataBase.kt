package ru.iuturakulov.mybudgetbackend.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

fun configureDatabase(config: ApplicationConfig) {
    DatabaseConfig.initDB(config)
}

object DatabaseConfig {
    fun initDB(config: ApplicationConfig) {
        println("Environment variables:")
        println("PG_USER: ${System.getenv("PG_USER")}")
        println("PG_PASSWORD: ${System.getenv("PG_PASSWORD")}")
        println("PG_DATABASE: ${System.getenv("PG_DATABASE")}")

        val dbUrl = System.getenv("DATABASE_URL") ?: config.property("database.url").getString()
        val dbUser = System.getenv("PG_USER") ?: config.property("database.user").getString()
        val dbPassword = System.getenv("PG_PASSWORD") ?: config.property("database.password").getString()
        val dbDriver = config.property("database.driver").getString()

        println("Using database configuration:")
        println("URL: $dbUrl")
        println("User: $dbUser")
        println("Driver: $dbDriver")

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            driverClassName = dbDriver
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 300000
            maxLifetime = 1200000
            connectionTimeout = 30000
            validationTimeout = 5000
            leakDetectionThreshold = 60000
        }
        val dataSource = HikariDataSource(hikariConfig)
//        runFlyway(dataSource)
        Database.connect(dataSource)
    }

    private fun runFlyway(dataSource: DataSource) {
        try {
            val flyway = Flyway
                .configure()
                .locations("classpath:db/migrations")
                .dataSource(dataSource)
                .load()
            flyway.migrate()
        } catch (e: FlywayException) {
            println("⚠ Ошибка миграции Flyway: ${e.message}")
            throw e
        }
    }
}
