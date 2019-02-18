package ru.dyatel.inuyama.utilities

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import hirondelle.date4j.DateTime
import org.jetbrains.anko.defaultSharedPreferences
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.overseer.OverseerConfiguration
import ru.dyatel.inuyama.rutracker.RutrackerConfiguration
import ru.dyatel.inuyama.transmission.TransmissionConfiguration
import java.util.TimeZone

private const val CONFIGURATION_OVERSEER_PERIOD = "overseer_period"
private const val CONFIGURATION_TRANSMISSION = "transmission_configuration"
private const val CONFIGURATION_RUTRACKER = "rutracker_configuration"

private const val DATA_LAST_CHECK = "overseer_last_check"
private const val DATA_DEVICE_IDENTIFIER = "device_identifier"

class PreferenceHelper(context: Context) : KodeinAware {

    override val kodein by closestKodein { context }
    private val gson by instance<Gson>()

    private val preferences = context.defaultSharedPreferences

    var overseer: OverseerConfiguration
        get() = OverseerConfiguration(preferences.getInt(CONFIGURATION_OVERSEER_PERIOD, 30))
        set(value) {
            preferences.editAndApply { putInt(CONFIGURATION_OVERSEER_PERIOD, value.period) }
        }

    var transmission: TransmissionConfiguration
        get() = gson.fromJson(preferences.getString(CONFIGURATION_TRANSMISSION, "{}")!!)
        set(value) {
            preferences.editAndApply { putString(CONFIGURATION_TRANSMISSION, gson.toJson(value)) }
        }

    var rutracker: RutrackerConfiguration
        get() = gson.fromJson(preferences.getString(CONFIGURATION_RUTRACKER, "{}")!!)
        set(value) {
            preferences.editAndApply { putString(CONFIGURATION_RUTRACKER, gson.toJson(value)) }
        }

    var lastCheck: DateTime?
        get() {
            return preferences.getLong(DATA_LAST_CHECK, -1)
                    .takeIf { it > 0 }
                    ?.let { DateTime.forInstant(it, TimeZone.getDefault()) }
        }
        set(value) {
            preferences.editAndApply { putLong(DATA_LAST_CHECK, value?.getMilliseconds(TimeZone.getDefault()) ?: -1) }
        }

    var deviceIdentifier: String?
        get() = preferences.getString(DATA_DEVICE_IDENTIFIER, null)
        set(value) {
            preferences.editAndApply { putString(DATA_DEVICE_IDENTIFIER, value) }
        }

}

fun SharedPreferences.editAndApply(edit: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.edit()
    editor.apply()
}