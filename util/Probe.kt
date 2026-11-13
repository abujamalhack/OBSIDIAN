package ai.obsidian.agent.util

import ai.obsidian.agent.root.Shell
import android.app.admin.DevicePolicyManager
import android.content.Context

object Probe {

    const val RUNG_NONE = 0
    const val RUNG_OWNER = 1
    const val RUNG_APPOPS = 2
    const val RUNG_ROOT = 3

    fun detect(context: Context): Int = when {
        Shell.rootAvailable() -> RUNG_ROOT
        appOpsGranted(context) -> RUNG_APPOPS
        isOwner(context) -> RUNG_OWNER
        else -> RUNG_NONE
    }

    private fun isOwner(context: Context): Boolean = runCatching {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        dpm != null && dpm.isDeviceOwnerApp(context.packageName)
    }.getOrDefault(false)

    private fun appOpsGranted(context: Context): Boolean = runCatching {
        val process = Runtime.getRuntime()
            .exec(arrayOf("appops", "get", context.packageName, "CAMERA"))
        process.inputStream.bufferedReader().readText().contains("allow")
    }.getOrDefault(false)

    fun describe(rung: Int): String = when (rung) {
        RUNG_ROOT -> "3 root"
        RUNG_APPOPS -> "2 appops"
        RUNG_OWNER -> "1 device-owner"
        else -> "0 none"
    }
}
