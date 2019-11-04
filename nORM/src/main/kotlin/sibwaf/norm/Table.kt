package sibwaf.norm

import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

private val idGenerator = AtomicInteger(1)
private fun generateSafeId(): String {
    return "norm${idGenerator.getAndIncrement()}"
}

class ColumnConfiguration {
    var generated = false
}

class Column<T> internal constructor(private val tableId: String, val name: String, internal val property: KProperty<T>, internal val generated: Boolean) {

    companion object {
        internal const val TEMPLATE_PREFIX = "!COLUMN"

        private val mutableById = mutableMapOf<String, Column<*>>()
        internal val byId = mutableById as Map<String, Column<*>>
    }

    private val columnId = generateSafeId()

    internal val propertyName by lazy { property.name }

    internal val getter by lazy { property.getter }
    internal val setter by lazy { (property as? KMutableProperty<T>)?.setter }

    init {
        mutableById[columnId] = this
    }

    override fun toString(): String {
        return "#{$TEMPLATE_PREFIX $tableId/$columnId}"
    }
}

abstract class Table<TEntity : Any>(val name: String, internal val type: KClass<TEntity>) {

    companion object {
        internal const val TEMPLATE_PREFIX = "!TABLE"

        private val mutableById = mutableMapOf<String, Table<*>>()
        internal val byId = mutableById as Map<String, Table<*>>
    }

    private val tableId = generateSafeId()

    // TODO: linked map?
    private val columns = mutableMapOf<String, Column<*>>()

    internal val orderedColumns by lazy { columns.keys.toList() }

    open internal val readableColumns by lazy { orderedColumns.map { columns[it]!! } }
    open internal val writeableColumns by lazy { orderedColumns.map { columns[it]!! }.filter { !it.generated } }

    val selColumns by lazy { readableColumns.joinToString(", ") }
    val insColumns by lazy { writeableColumns.joinToString(", ") }

    init {
        @Suppress("LeakingThis")
        mutableById[tableId] = this
    }

    protected fun <T> column(name: String, property: KProperty<T>, configure: ColumnConfiguration.() -> Unit = {}): Column<T> {
        if (name in columns) {
            throw IllegalArgumentException("Column $name already exists")
        }

        val configuration = ColumnConfiguration()
        configuration.configure()

        return Column(
                tableId = tableId,
                name = name,
                property = property,
                generated = configuration.generated
        ).also { columns[name] = it }
    }

    fun findColumn(name: String): Column<*>? {
        return columns[name]
    }

    override fun toString(): String {
        return "#{$TEMPLATE_PREFIX $tableId}"
    }
}

abstract class KeyedTable<TKey, TEntity : Any>(name: String, type: KClass<TEntity>) : Table<TEntity>(name, type) {
    abstract val key: Column<TKey>
}
