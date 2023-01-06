package ru.dyatel.inuyama.utilities

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hirondelle.date4j.DateTime
import org.jetbrains.anko.defaultSharedPreferences
import ru.dyatel.inuyama.overseer.OverseerConfiguration
import ru.dyatel.inuyama.pairing.SavedPairedServer
import ru.dyatel.inuyama.rutracker.RutrackerConfiguration
import ru.sibwaf.inuyama.common.Pairing
import ru.sibwaf.inuyama.common.paired.model.ErrorLogEntry
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import java.security.KeyPair
import java.util.TimeZone

private const val CONFIGURATION_OVERSEER_PERIOD = "overseer_period"
@Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
private const val CONFIGURATION_TRANSMISSION = "transmission_configuration"
private const val CONFIGURATION_RUTRACKER = "rutracker_configuration"
private const val CONFIGURATION_DISCOVERY_PORT = "discovery_port"

private const val DATA_LAST_USED_VERSION = "last_used_version"
private const val DATA_LAST_CHECK = "overseer_last_check"
private const val DATA_DEVICE_KEYPAIR = "rsa_keypair"
private const val DATA_DEVICE_IDENTIFIER = "device_identifier"
private const val DATA_PAIRED_SERVER = "paired_server"
private const val DATA_ERROR_LOG = "error_log"

class PreferenceHelper(
    context: Context,
    private val gson: Gson,
) {

    private val preferences = context.defaultSharedPreferences

    var lastUsedVersion: Int
        get() = preferences.getInt(DATA_LAST_USED_VERSION, -1)
        set(value) = preferences.editAndApply { putInt(DATA_LAST_USED_VERSION, value) }

    var overseer: OverseerConfiguration
        get() = OverseerConfiguration(preferences.getInt(CONFIGURATION_OVERSEER_PERIOD, 30))
        set(value) {
            preferences.editAndApply { putInt(CONFIGURATION_OVERSEER_PERIOD, value.period) }
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

    var discoveryPort: Int
        get() = preferences.getInt(CONFIGURATION_DISCOVERY_PORT, Pairing.DEFAULT_DISCOVER_SERVER_PORT)
        set(value) {
            preferences.editAndApply { putInt(CONFIGURATION_DISCOVERY_PORT, value) }
        }

    var keyPair: KeyPair?
        get() = preferences.getString(DATA_DEVICE_KEYPAIR, null)?.let { Encoding.decodeRSAKeyPair(Encoding.decodeBase64(it)) }
        set(value) {
            preferences.editAndApply { putString(DATA_DEVICE_KEYPAIR, value?.let { Encoding.encodeBase64(Encoding.encodeRSAKeyPair(it)) }) }
        }

    var deviceIdentifier: String?
        get() = preferences.getString(DATA_DEVICE_IDENTIFIER, null)
        set(value) {
            preferences.editAndApply { putString(DATA_DEVICE_IDENTIFIER, value) }
        }

    var pairedServer: SavedPairedServer?
        get() = preferences.getString(DATA_PAIRED_SERVER, null)?.let { gson.fromJson(it) }
        set(value) {
            preferences.editAndApply { putString(DATA_PAIRED_SERVER, value?.let { gson.toJson(it) }) }
        }

    var errorLog: List<ErrorLogEntry>
        get() = preferences.getString(DATA_ERROR_LOG, null)?.let {
            gson.fromJson(it, object : TypeToken<List<ErrorLogEntry>>() {}.type)
        } ?: emptyList()
        set(value) = preferences.editAndApply { putString(DATA_ERROR_LOG, gson.toJson(value)) }
}

fun SharedPreferences.editAndApply(edit: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.edit()
    editor.apply()
}
