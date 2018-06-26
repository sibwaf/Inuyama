package ru.dyatel.inuyama

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import org.jetbrains.anko.defaultSharedPreferences
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.overseer.OverseerConfiguration
import ru.dyatel.inuyama.transmission.TransmissionConfiguration

private const val CONFIGURATION_OVERSEER_PERIOD = "overseer_period"
private const val CONFIGURATION_TRANSMISSION = "transmission_configuration"

class PreferenceHelper(context: Context) : KodeinAware {

    override val kodein by closestKodein { context }
    private val gson by instance<Gson>()

    private val preferences = context.defaultSharedPreferences

    var overseer: OverseerConfiguration
        get() = OverseerConfiguration(preferences.getInt(CONFIGURATION_OVERSEER_PERIOD, 30))
        set(value) {
            preferences.editAndApply {
                putInt(CONFIGURATION_OVERSEER_PERIOD, value.period)
            }
        }

    var transmission: TransmissionConfiguration
        get() = gson.fromJson(preferences.getString(CONFIGURATION_TRANSMISSION, "{}"))
        set(value) {
            preferences.editAndApply {
                putString(CONFIGURATION_TRANSMISSION, gson.toJson(value))
            }
        }

}

fun SharedPreferences.editAndApply(edit: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.edit()
    editor.apply()
}
