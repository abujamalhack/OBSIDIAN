package ai.obsidian.agent

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Config(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("obsidian.cfg", Context.MODE_PRIVATE)

    var enrolled: Boolean
        get() = prefs.getBoolean("enrolled", false)
        set(value) = prefs.edit().putBoolean("enrolled", value).apply()

    val relayBase: String get() = prefs.getString("relay", "").orEmpty().trimEnd('/')
    val relayHost: String
        get() = relayBase.substringAfter("://").substringBefore('/')
    val deviceId: String get() = prefs.getString("dev", "").orEmpty()
    val apiKey: String get() = prefs.getString("key", "").orEmpty()
    val opPub: String get() = prefs.getString("oppub", "").orEmpty()
    val chat: String get() = prefs.getString("chat", "").orEmpty()
    val bearer: String get() = prefs.getString("bearer", "").orEmpty()

    var otpEnabled: Boolean
        get() = prefs.getBoolean("otp", true)
        set(value) = prefs.edit().putBoolean("otp", value).apply()

    var resurrect: Boolean
        get() = prefs.getBoolean("resurrect", true)
        set(value) = prefs.edit().putBoolean("resurrect", value).apply()

    val rung: Int get() = prefs.getInt("rung", 0)

    fun setRung(value: Int) = prefs.edit().putInt("rung", value).apply()

    fun save(
        relay: String,
        deviceId: String,
        apiKey: String,
        opPub: String,
        chat: String
    ) {
        prefs.edit()
            .putString("relay", relay)
            .putString("dev", deviceId)
            .putString("key", apiKey)
            .putString("oppub", opPub)
            .putString("chat", chat)
            .putString("bearer", deriveBearer(apiKey, deviceId))
            .apply()
    }

    companion object {
        fun deriveBearer(apiKey: String, deviceId: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(apiKey.toByteArray(), "HmacSHA256"))
            val digest = mac.doFinal("obsidian/bearer/v1/$deviceId".toByteArray())
            return Base64.encodeToString(
                digest,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        }
    }
}
