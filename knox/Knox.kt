package ai.obsidian.agent.knox

import android.content.Context
import android.os.Build

object Knox {

    private const val ENTERPRISE = "com.samsung.android.knox.EnterpriseDeviceManager"
    private const val PERSONA = "com.samsung.android.knox.SemPersonaManager"

    private val POLICY_METHODS = listOf(
        "isCameraEnabled",
        "isMicrophoneEnabled",
        "isMockLocationEnabled",
        "isWifiEnabled",
        "isUsbDebuggingEnabled",
        "isSdCardEnabled"
    )

    fun available(): Boolean = runCatching { Class.forName(ENTERPRISE) }.isSuccess

    fun run(context: Context, sub: String): String = when (sub) {
        "probe" -> probe()
        "persona" -> persona(context)
        "policy" -> policy()
        "attest" -> attest()
        else -> "knox probe|persona|policy|attest"
    }

    private fun probe(): String = buildString {
        appendLine("knox_sdk=${available()}")
        appendLine("knox_flag=${property("ro.config.knox")}")
        appendLine("warranty_bit=${property("ro.boot.warranty_bit")}")
        appendLine("kg_state=${property("ro.boot.kg.state")}")
        appendLine("verified_boot=${property("ro.boot.verifiedbootstate")}")
        appendLine("flash_locked=${property("ro.boot.flash.locked")}")
        appendLine("sales_code=${property("ro.csc.sales_code")}")
        appendLine("patch=${Build.VERSION.SECURITY_PATCH}")
        append("model=${Build.MODEL} fw=${Build.DISPLAY}")
    }

    private fun persona(context: Context): String = runCatching {
        val clazz = Class.forName(PERSONA)
        val instance = clazz.getConstructor(Context::class.java).newInstance(context)
        val ids = clazz.getMethod("getKnoxIds").invoke(instance) as? IntArray
            ?: return@runCatching "no containers"

        val builder = StringBuilder("containers=${ids.size}\n")
        for (id in ids) {
            val admin = clazz
                .getMethod("isKnoxId", Int::class.javaPrimitiveType)
                .invoke(instance, id)
            builder.appendLine("id=$id admin=$admin")
        }
        builder.toString()
    }.getOrElse { "persona fail: ${it.message}" }

    private fun policy(): String = runCatching {
        val clazz = Class.forName(ENTERPRISE)
        val edm = clazz.getMethod("getInstance").invoke(null)
        val policy = clazz.getMethod("getAdvancedRestrictionPolicy").invoke(edm)

        val builder = StringBuilder()
        for (method in POLICY_METHODS) {
            runCatching {
                builder.appendLine("$method=${policy.javaClass.getMethod(method).invoke(policy)}")
            }
        }
        builder.toString()
    }.getOrElse { "policy fail: ${it.message}" }

    private fun attest(): String = buildString {
        appendLine("attestation inputs (verified server-side, not forgeable):")
        appendLine("fingerprint=${Build.FINGERPRINT}")
        appendLine("verified_boot=${property("ro.boot.verifiedbootstate")}")
        appendLine("warranty_bit=${property("ro.boot.warranty_bit")}")
        append("knox_flag=${property("ro.config.knox")}")
    }

    private fun property(key: String): String = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        clazz.getMethod("get", String::class.java).invoke(null, key) as String
    }.getOrElse { "?" }
}
