package sibwaf.norm

import java.sql.Timestamp
import kotlin.system.measureTimeMillis

abstract class Migration {
    abstract val version: Int
    abstract fun execute(queryRunner: QueryRunner)
}

class SqlMigration(override val version: Int, private val sql: String) : Migration() {
    override fun execute(queryRunner: QueryRunner) {
        queryRunner.executeUpdate(RawQuery(sql))
    }
}

object MigrationExecutor {

    private const val TABLE_NAME = "norm_migration_history"

    private const val VERSION_COLUMN = "schema_version"
    private const val APPLIED_AT_COLUMN = "applied_at"
    private const val MIGRATION_TIME_COLUMN = "migration_time"

    private val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $VERSION_COLUMN INT PRIMARY KEY,
            $APPLIED_AT_COLUMN TIMESTAMP,
            $MIGRATION_TIME_COLUMN INT
        )
    """.trimIndent()

    fun execute(queryRunner: QueryRunner, migrations: Collection<Migration>) {
        queryRunner.executeUpdate(CREATE_TABLE)

        val appliedMigrations = queryRunner
                .executeQuery("SELECT $VERSION_COLUMN FROM $TABLE_NAME")
                .map { it.data.single() as Int }
                .toSet()

        val pendingMigrations = migrations
                .filter { it.version !in appliedMigrations }
                .sortedBy { it.version }
                .takeIf { it.isNotEmpty() } ?: return

        if (appliedMigrations.isNotEmpty()) {
            val maxApplied = appliedMigrations.max()!!
            val minPending = pendingMigrations.minBy { it.version }!!.version

            check(minPending > maxApplied) {
                "Migration #$minPending is not installed when schema has version #$maxApplied; prepending migrations is not allowed"
            }
        }

        for (migration in pendingMigrations) {
            val time = measureTimeMillis {
                migration.execute(queryRunner)
            }

            val query = RawQuery("INSERT INTO $TABLE_NAME ($VERSION_COLUMN, $APPLIED_AT_COLUMN, $MIGRATION_TIME_COLUMN) VALUES (?, ?, ?)")
            query.addParameter(migration.version)
            query.addParameter(Timestamp(System.currentTimeMillis()))
            query.addParameter(time)
            queryRunner.executeUpdate(query)
        }
    }
}
