package com.eilaji.backend.data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp as jdaTimestamp
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Named, idempotent schema migrations.
 *
 * Every migration records its name in schema_migrations, so it runs once per database and stays
 * safe to re-run. `SchemaUtils.createMissingTablesAndColumns` is kept for a fresh local database
 * only; anything that changes an existing column lives here, which makes upgrades reviewable.
 */
object Migrations {
    private val logger = LoggerFactory.getLogger(Migrations::class.java)

    class AppliedMigrations(name: String) : Table(name) {
        val id = uuid("id").autoGenerate()
        val migration = varchar("migration", 200)
        val appliedAt = jdaTimestamp("applied_at").clientDefault { Instant.now() }
        override val primaryKey = PrimaryKey(id)
    }

    private val table = AppliedMigrations("schema_migrations")

    /** Migrations that may fail without breaking startup (missing extension, restricted platform). */
    private val OPTIONAL_MIGRATIONS = setOf("011_search_trigram")

    private val migrations: List<Pair<String, String>> = listOf(
        "001_orders_prescription_nullable" to """
            ALTER TABLE orders ALTER COLUMN prescription_id DROP NOT NULL
        """.trimIndent(),
        "002_orders_indexes" to """
            CREATE INDEX IF NOT EXISTS idx_orders_patient_created ON orders (patient_id, created_at DESC)
        """.trimIndent(),
        "003_orders_pharmacy_status_index" to """
            CREATE INDEX IF NOT EXISTS idx_orders_pharmacy_status ON orders (pharmacy_id, status)
        """.trimIndent(),
        "004_medicines_title_indexes" to """
            CREATE INDEX IF NOT EXISTS idx_medicines_title_en ON medicines (title_en)
        """.trimIndent(),
        "005_pharmacies_active_index" to """
            CREATE INDEX IF NOT EXISTS idx_pharmacies_active ON pharmacies (is_active)
        """.trimIndent(),
        "006_favorites_unique_index" to """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_favorites_user_medicine
            ON favorites (user_id, medicine_id, COALESCE(pharmacy_id, '00000000-0000-0000-0000-000000000000'))
        """.trimIndent(),
        "007_prescriptions_pharmacy_index" to """
            CREATE INDEX IF NOT EXISTS idx_prescriptions_user_status ON prescriptions (user_id, status)
        """.trimIndent(),
        "008_chats_pharmacy_nullable" to MIGRATION_008,
        "009_chats_sentinel_cleanup" to MIGRATION_008_CLEANUP,
        "010_order_items" to MIGRATION_010,
        // Optional: needs the pg_trgm extension (created by init-db.sql). Skipped with a warning
        // when the extension is unavailable, e.g. on a managed database without superuser rights.
        "011_search_trigram" to MIGRATION_110
    )

    fun run() {
        transaction {
            SchemaUtils.create(table)
            val applied = table.selectAll().map { it[table.migration] }.toSet()
            migrations.forEach { (name, sql) ->
                if (applied.contains(name)) return@forEach
                try {
                    exec(sql)
                    table.insert {
                        it[table.id] = UUID.randomUUID()
                        it[table.migration] = name
                        it[table.appliedAt] = Instant.now()
                    }
                    logger.info("Applied migration $name")
                } catch (e: Exception) {
                    if (OPTIONAL_MIGRATIONS.contains(name)) {
                        logger.warn("Optional migration $name not applied: ${e.message}")
                        return@forEach
                    }
                    // A migration that cannot run is a startup failure, never a silent skip.
                    logger.error("Migration $name failed", e)
                    throw e
                }
            }
        }
    }
}

/**
 * Older databases stored a sentinel uuid (00000000-...) for chats without a pharmacy and the
 * column was NOT NULL, which made those rows impossible to insert at all.
 * This makes the pharmacy side nullable and converts the sentinels to NULL.
 */
private const val MIGRATION_008 = """
    ALTER TABLE chats ALTER COLUMN pharmacy_user_id DROP NOT NULL
"""

private const val MIGRATION_008_CLEANUP = """
    UPDATE chats SET pharmacy_user_id = NULL WHERE pharmacy_user_id = '00000000-0000-0000-0000-000000000000'
"""

/** Line items per order: what was bought, at what price. Makes receipts and restocking possible. */
private const val MIGRATION_010 = """
    CREATE TABLE IF NOT EXISTS order_items (
        id UUID PRIMARY KEY,
        order_id UUID NOT NULL,
        medicine_id UUID,
        medicine_name VARCHAR(255) NOT NULL,
        quantity INTEGER NOT NULL,
        unit_price DECIMAL(12, 2) NOT NULL,
        line_total DECIMAL(12, 2) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
    )
"""

/** Trigram indexes make the leading-wildcard medicine search use an index instead of a scan. */
private const val MIGRATION_110 = """
    CREATE INDEX IF NOT EXISTS idx_medicines_title_en_trgm ON medicines USING gin (title_en gin_trgm_ops)
"""
