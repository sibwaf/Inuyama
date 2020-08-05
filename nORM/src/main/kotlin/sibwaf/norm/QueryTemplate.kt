package sibwaf.norm

import kotlin.reflect.KClass

class QueryTemplate(private val context: NormContext, query: String) {

    private companion object {
        val TEMPLATE_PATTERN = Regex("""#\{\s*(.*?)\s*\}""")

        fun replaceTemplates(text: String, replacer: (String) -> String?): String {
            var result = text
            var startIndex = 0

            while (true) {
                val match = TEMPLATE_PATTERN.find(result, startIndex) ?: break
                val template = match.groupValues[1]

                val replacement = replacer(template)
                if (replacement != null) {
                    result = result.replaceRange(match.range, replacement)
                    startIndex = match.range.first + replacement.length
                } else {
                    startIndex = match.range.last + 1
                }
            }

            return result
        }
    }

    private val query: String
    private val parameters = mutableMapOf<String, Any>()

    init {
        // TODO: table aliases
        // Possible solution: count referenced tables in query, >1 => add aliases

        this.query = replaceTemplates(query) {
            when {
                it.startsWith(Table.TEMPLATE_PREFIX) -> {
                    val tableId = it.removePrefix("${Table.TEMPLATE_PREFIX} ")
                    val table = Table.byId.getValue(tableId)
                    "`${table.name}`"
                }
                it.startsWith(Column.TEMPLATE_PREFIX) -> {
                    val (tableId, columnId) = it.removePrefix("${Column.TEMPLATE_PREFIX} ").split("/")
                    val column = Column.byId.getValue(columnId)
                    "`${column.name}`"
                }
                else -> null
            }
        }
    }

    fun withParameter(name: String, value: Any): QueryTemplate {
        parameters[name] = value
        return this
    }

    private fun toRawQuery(): RawQuery {
        val orderedParameters = mutableListOf<Any?>()
        val query = replaceTemplates(query) {
            val (shouldExplode, name) = if (it.startsWith("!EXPLODE")) {
                true to it.removePrefix("!EXPLODE").trimStart()
            } else {
                false to it
            }

            check(name in parameters) { "No value for parameter $name" }
            val value = parameters[name]!!

            // TODO: handle nulls
            if (shouldExplode) {
                @Suppress("UNCHECKED_CAST")
                val mappers = context.getMappers(value::class) as Mappers<Any>
                orderedParameters.addAll(mappers.fromObject(value))
                (0 until mappers.fromObjectSize).joinToString(", ") { "?" }
            } else {
                val table = context.findTable(value::class)

                orderedParameters += when (table) {
                    is KeyedTable<*, *> -> table.key.getter.call(value)
                    is Table<*> -> throw IllegalArgumentException("Parameter $name")
                    else -> value
                }

                "?"
            }
        }.let { RawQuery(it) }

        for (parameter in orderedParameters) {
            query.addParameter(parameter)
        }

        return query
    }

    fun executeUpdate() {
        context.queryRunner.executeUpdate(toRawQuery())
    }

    fun <T : Any> execute(type: KClass<T>): Iterable<T> {
        val mapper = context.getMappers(type).toObject

        val rawQuery = toRawQuery()
        val result = context.queryRunner.executeQuery(rawQuery)

        return result.map { mapper(it.data.toTypedArray()) }
    }
}
