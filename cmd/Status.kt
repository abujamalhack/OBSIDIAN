package ai.obsidian.agent.cmd

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.root.Shell
import ai.obsidian.agent.util.Probe
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager

object Status {

    private val WATCHED_PERMISSIONS = listOf(
        "CAMERA",
        "RECORD_AUDIO",
        "ACCESS_FINE_LOCATION",
        "ACCESS_BACKGROUND_LOCATION",
        "READ_SMS"
    )

    fun render(context: Context): String {
        val app = context.applicationContext as AgentApp
        val power = context.getSystemService(PowerManager::class.java)

        return buildString {
            appendLine("model=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
            appendLine("patch=${Build.VERSION.SECURITY_PATCH}")
            appendLine("build=${Build.DISPLAY}")
            appendLine("rung=${Probe.describe(app.cfg.rung)}")
            appendLine("root=${Shell.rootAvailable()}")
            appendLine("selinux=${Shell.run(context, "getenforce").trim()}")
            appendLine("doze_exempt=${power.isIgnoringBatteryOptimizations(context.packageName)}")
            appendLine("bucket=${power.appStandbyBucket}")
            appendLine("audit=${app.audit.verify()}")
            for (permission in WATCHED_PERMISSIONS) {
                val granted = context.checkSelfPermission("android.permission.$permission") ==
                    PackageManager.PERMISSION_GRANTED
                appendLine("perm.$permission=$granted")
            }
            append("otp=${app.cfg.otpEnabled} resurrect=${app.cfg.resurrect}")
        }
    }
}
