package com.eilaji.backend.config

import com.typesafe.config.ConfigFactory
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object DatabaseConfig {
    private val config = ConfigFactory.load()

    val url: String = config.getString("database.url")
    val driver: String = config.getString("database.driver")
    val user: String = config.getString("database.user")
    val password: String = config.getString("database.password")
    val poolSize: Int = config.getInt("database.poolSize")
    val sqlLogging: Boolean = config.getBoolean("logging.sqlLogging")

    private var dataSource: HikariDataSource? = null

    fun init() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = driver
            username = user
            password = password
            maximumPoolSize = poolSize
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        dataSource = HikariDataSource(hikariConfig)

        Database.connect(dataSource!!)

        if (sqlLogging) {
            println("WARNING: SQL logging requested but not configured for Exposed 0.55")
        }

        println("Database connection initialized successfully")
    }

    fun close() {
        dataSource?.close()
        println("Database connection closed")
    }
}
