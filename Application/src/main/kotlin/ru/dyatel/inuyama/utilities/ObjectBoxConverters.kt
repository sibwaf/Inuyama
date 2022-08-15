package ru.dyatel.inuyama.utilities

import hirondelle.date4j.DateTime
import io.objectbox.converter.PropertyConverter
import java.util.UUID

class DateTimeConverter : PropertyConverter<DateTime?, String?> {
    override fun convertToDatabaseValue(entityProperty: DateTime?): String? {
        if (entityProperty == null) {
            return null
        }

        val copy = DateTime(
            entityProperty.year,
            entityProperty.month ?: 1,
            entityProperty.day ?: 1,
            entityProperty.hour ?: 0,
            entityProperty.minute ?: 0,
            entityProperty.second ?: 0,
            entityProperty.nanoseconds ?: 0,
        )
        return copy.format("YYYY-MM-DDThh:mm:ss")
    }

    override fun convertToEntityProperty(databaseValue: String?) = databaseValue?.let { DateTime(it) }
}

class UuidConverter : PropertyConverter<UUID?, String?> {
    override fun convertToDatabaseValue(entityProperty: UUID?) = entityProperty?.toString()
    override fun convertToEntityProperty(databaseValue: String?) = databaseValue?.let { UUID.fromString(it) }
}
