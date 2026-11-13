package ai.obsidian.agent.ui

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.svc.C2Service
import ai.obsidian.agent.svc.GuardService
import ai.obsidian.agent.util.Probe
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.TextView

class EnrollActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AgentApp

        intent?.data?.let { uri ->
            if (uri.scheme == "obsidian" && uri.host == "enroll") {
                val relay = uri.getQueryParameter("relay")
                val deviceId = uri.getQueryParameter("dev")
                val apiKey = uri.getQueryParameter("key")
                val operatorPub = uri.getQueryParameter("oppub")
                val chat = uri.getQueryParameter("chat")

                if (relay != null && deviceId != null && apiKey != null &&
                    operatorPub != null && chat != null
                ) {
                    app.onboard(relay, deviceId, apiKey, operatorPub, chat)
                    C2Service.start(this)
                    GuardService.start(this)
                }
            }
        }

        val view = TextView(this)
        view.text = if (app.cfg.enrolled) {
            buildString {
                appendLine("enrolled")
                appendLine("device ${app.cfg.deviceId}")
                appendLine("rung ${Probe.describe(app.cfg.rung)}")
                appendLine("relay ${app.cfg.relayBase}")
                append("audit ${app.audit.verify()}")
            }
        } else {
            "not enrolled\nopen an obsidian://enroll?... URI"
        }
        setContentView(view)
    }
}
