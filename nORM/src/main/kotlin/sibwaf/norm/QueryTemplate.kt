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

    private enum class ParameterType {
        REGULAR, EXPLODED, EQUALITY_CHECK, INEQUALITY_CHECK
    }

    private val query: String
    private val parameters = mutableMapOf<String, Any?>()

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

    fun withParameter(name: String, value: Any?): QueryTemplate {
        parameters[name] = value
        return this
    }

    private fun getParameter(template: String): Pair<ParameterType, String> {
        return when {
            template.startsWith("!EXPLODE") -> {
                ParameterType.EXPLODED to template.removePrefix("!EXPLODE").trimStart()
            }
            template.startsWith("=") -> {
                ParameterType.EQUALITY_CHECK to template.removePrefix("=").trimStart()
            }
            template.startsWith("!=") -> {
                ParameterType.INEQUALITY_CHECK to template.removePrefix("!=").trimStart()
            }
            else -> {
                ParameterType.REGULAR to template
            }
        }
    }

    private fun toRawQuery(): RawQuery {
        val orderedParameters = mutableListOf<Any?>()
        val query = replaceTemplates(query) { template ->
            val (parameterType, parameterName) = getParameter(template)

            check(parameterName in parameters) { "No value for parameter $parameterName" }
            val value = parameters[parameterName]
            check(value != null || parameterType == ParameterType.EQUALITY_CHECK || parameterType == ParameterType.INEQUALITY_CHECK)

            val valueType = if (value != null) value::class else null

            if (parameterType == ParameterType.EXPLODED) {
                require(value != null && valueType != null) { "Can't explode null-parameter $parameterName" }

                @Suppress("UNCHECKED_CAST")
                val mappers = context.getMappers(valueType) as Mappers<Any>
                orderedParameters.addAll(mappers.fromObject(value))
                (0 until mappers.fromObjectSize).joinToString(", ") { "?" }
            } else {
                val table = valueType?.let { context.findTable(it) }

                if (table is KeyedTable<*, *>) {
                    if (value != null) {
                        orderedParameters += table.key.getter.call(value)
                        return@replaceTemplates "?"
                    }
                } else if (table != null) {
                    throw IllegalArgumentException("Parameter $parameterName can only be exploded")
                }

                if (value == null) {
                    if (parameterType == ParameterType.EQUALITY_CHECK) {
                        "IS NULL"
                    } else {
                        "IS NOT NULL"
                    }
                } else {
                    orderedParameters += value
                    "?"
                }
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

    inline fun <reified T : Any> execute() = execute(T::class)
}
