package sibwaf.norm

import kotlin.reflect.full.valueParameters

internal class Mappers<T : Any> private constructor(
        val toObject: (Array<Any?>) -> T, val toObjectSize: Int,
        val fromObject: (T) -> Array<Any?>, val fromObjectSize: Int
) {

    companion object {
        private fun <T : Any> createToObjectMapper(table: Table<T>): (Array<Any?>) -> T {
            val columns = table.readableColumns
            val propertyNames = columns.map { it.propertyName }

            val constructor = table.type.constructors
                    .sortedByDescending { it.valueParameters.size }
                    .firstOrNull { it.valueParameters.all { parameter -> parameter.name in propertyNames } }
                    ?: throw IllegalStateException("Failed to find a suitable constructor for type ${table.type}")

            val (constructorProperties, nonConstructorProperties) = propertyNames.partition { name ->
                constructor.valueParameters.any { it.name == name }
            }

            val propertyIndexByName = columns.mapIndexed { index, column -> column.propertyName to index }.toMap()
            val constructorPropertyIndices = constructorProperties.map { propertyIndexByName.getValue(it) }

            return { row ->
                val constructorValues = constructorPropertyIndices.map { row[it] }.toTypedArray()

                // TODO: generic implementation
                // TODO: custom converters in table
                for ((index, parameter) in constructor.parameters.withIndex()) {
                    if (parameter.type.classifier == Boolean::class && constructorValues[index] is Number) {
                        constructorValues[index] = (constructorValues[index] as Number).toLong() != 0L
                    }
                }

                val instance = constructor.call(*constructorValues)

                for (property in nonConstructorProperties) {
                    val index = propertyIndexByName.getValue(property)
                    columns[index].setter!!.call(instance, row[index])
                }

                instance
            }
        }

        private fun <T : Any> createFromObjectMapper(table: Table<T>): (T) -> Array<Any?> {
            val getters = table.writeableColumns.map { it.getter }
            return { entity -> getters.map { it.call(entity) }.toTypedArray() }
        }

        fun <T : Any> create(table: Table<T>): Mappers<T> {
            return Mappers(
                    toObject = createToObjectMapper(table),
                    toObjectSize = table.readableColumns.size,
                    fromObject = createFromObjectMapper(table),
                    fromObjectSize = table.writeableColumns.size
            )
        }
    }

}