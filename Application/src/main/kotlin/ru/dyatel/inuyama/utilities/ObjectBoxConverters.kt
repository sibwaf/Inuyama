package ru.dyatel.inuyama.utilities

import hirondelle.date4j.DateTime
import io.objectbox.converter.PropertyConverter
import java.util.UUID

class DateTimeConverter : PropertyConverter<DateTime?, String?> {
    override fun convertToDatabaseValue(entityProperty: DateTime?) = entityProperty?.toString()
    override fun convertToEntityProperty(databaseValue: String?) = databaseValue?.let { DateTime(it) }
}

class UuidConverter : PropertyConverter<UUID?, String?> {
    override fun convertToDatabaseValue(entityProperty: UUID?) = entityProperty?.toString()
    override fun convertToEntityProperty(databaseValue: String?) = databaseValue?.let { UUID.fromString(it) }
}
