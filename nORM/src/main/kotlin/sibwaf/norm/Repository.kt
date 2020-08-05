package sibwaf.norm

abstract class Repository<TEntity : Any>(protected val context: NormContext) {

    protected abstract val table: Table<TEntity>

    protected val queryRunner
        get() = context.queryRunner

    init {
        // TODO: check for table registration?
    }

    open fun findAll(): Iterable<TEntity> {
        return with(table) {
            context.createQueryTemplate("SELECT $selColumns FROM $this")
                    .execute(type)
        }
    }

    open fun insert(entity: TEntity) {
        return with(table) {
            context.createQueryTemplate("INSERT INTO $this ($insColumns) VALUES (#{!EXPLODE entity})")
                    .withParameter("entity", entity)
                    .executeUpdate()
        }
    }
}

abstract class KeyedRepository<TKey : Any, TEntity : Any>(context: NormContext) : Repository<TEntity>(context) {

    abstract override val table: KeyedTable<TKey, TEntity>

    open fun findByKey(keyValue: TKey): TEntity? {
        return with(table) {
            context.createQueryTemplate("SELECT $selColumns FROM $this WHERE $key = #{key} LIMIT 1")
                    .withParameter("key", keyValue)
                    .execute(type)
                    .singleOrNull()
        }
    }

    open fun update(entity: TEntity): TEntity? {
        return with(table) {
            val columns = writeableColumns
                    .mapIndexed { index, column -> "$column = #{param$index}" }
                    .joinToString(", ")

            val query = context.createQueryTemplate("UPDATE $this SET $columns")
            for ((index, column) in writeableColumns.withIndex()) {
                query.withParameter("param$index", column.getter.call(entity))
            }
            query.executeUpdate()

            entity
        }
    }

    open fun deleteByKey(keyValue: TKey) {
        with(table) {
            context.createQueryTemplate("DELETE FROM $this WHERE $key = #{key}")
                    .withParameter("key", keyValue)
                    .executeUpdate()
        }
    }

    open fun delete(entity: TEntity) {
        val key = table.key.getter.call(entity)
        deleteByKey(key)
    }
}