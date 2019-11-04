package sibwaf.norm

import kotlin.reflect.KClass

class NormContextConfiguration internal constructor() {

    internal val tables = mutableMapOf<KClass<*>, Table<*>>()
    internal val migrations = mutableMapOf<Int, Migration>()

    var queryRunner: QueryRunner? = null

    var disableMigrations = false

    fun register(table: Table<*>) {
        check(table.type !in tables) { "Table for type ${table.type} is already registered" }
        tables[table.type] = table
    }

    fun register(migration: Migration) {
        check(migration.version !in migrations) { "Migration ${migration.version} is already registered" }
        migrations[migration.version] = migration
    }
}

class NormContext private constructor(
        val queryRunner: QueryRunner,
        private val tables: Map<KClass<*>, Table<*>>
) {

    companion object {
        fun create(configure: NormContextConfiguration.() -> Unit): NormContext {
            val configuration = NormContextConfiguration()
            configuration.configure()

            val context = NormContext(
                    configuration.queryRunner ?: throw IllegalArgumentException("No QueryRunner specified"),
                    configuration.tables.toMap()
            )

            if (!configuration.disableMigrations) {
                MigrationExecutor.execute(context.queryRunner, configuration.migrations.values)
            }

            return context
        }
    }

    private val mappers = mutableMapOf<KClass<*>, Mappers<*>>()

    internal fun <T : Any> findTable(type: KClass<T>): Table<T>? {
        @Suppress("UNCHECKED_CAST")
        return tables[type] as Table<T>?
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> getMappers(type: KClass<T>): Mappers<T> {
        return mappers.getOrPut(type) {
            val table = findTable(type) ?: throw IllegalStateException("Table for type $type is not registered")
            Mappers.create(table)
        } as Mappers<T>
    }

    fun createQueryTemplate(query: String) = QueryTemplate(this, query)
}