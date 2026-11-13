package ai.obsidian.agent.cmd

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.util.Probe
import android.content.Context
import android.os.Build
import java.time.Instant

object Report {

    fun build(context: Context, format: String): String {
        val app = context.applicationContext as AgentApp

        val markdown = buildString {
            appendLine("# OBSIDIAN engagement report")
            appendLine()
            appendLine("generated=${Instant.now()}")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
            appendLine("patch=${Build.VERSION.SECURITY_PATCH}")
            appendLine("fingerprint=${Build.FINGERPRINT}")
            appendLine("rung=${Probe.describe(app.cfg.rung)}")
            appendLine()
            appendLine("## Posture")
            appendLine("```")
            appendLine(Status.render(context))
            appendLine("```")
            appendLine()
            appendLine("## Findings")
            val findings = app.store.file("findings.md")
            appendLine(if (findings.exists()) findings.readText() else "_none captured_")
            appendLine()
            appendLine("## Audit chain")
            appendLine("verify=${app.audit.verify()}")
            appendLine()
            appendLine("## Evidence (audit tail)")
            appendLine("```")
            appendLine(app.audit.tail(200))
            appendLine("```")
        }

        val target = app.store.file("report_${System.currentTimeMillis()}.md")
        target.writeText(markdown)

        if (format == "path") return target.absolutePath
        return markdown
    }
}
