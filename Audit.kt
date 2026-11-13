package ai.obsidian.agent

import android.content.Context
import java.io.File
import java.security.MessageDigest

class Audit(context: Context) {

    private val log = File(context.filesDir, "audit.log")
    private val head = File(context.filesDir, "audit.head")

    private fun digest(previous: String, line: String): String {
        val sha = MessageDigest.getInstance("SHA-256")
        return sha.digest("$previous$line".toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun write(level: String, tag: String, message: String) {
        val previous = if (head.exists()) head.readText().trim() else "0".repeat(64)
        val line = "${System.currentTimeMillis()} $level $tag ${message.take(500)}"
        val next = digest(previous, line)
        log.appendText("$line $next\n")
        head.writeText(next)
    }

    fun info(tag: String, message: String) = write("I", tag, message)

    fun warn(tag: String, message: String) = write("W", tag, message)

    @Synchronized
    fun tail(count: Int): String {
        val chain = if (head.exists()) head.readText().trim() else "-"
        val body = if (log.exists()) log.readLines().takeLast(count).joinToString("\n") else ""
        return "head=$chain\n$body"
    }

    @Synchronized
    fun verify(): String {
        if (!log.exists()) return "EMPTY"
        var previous = "0".repeat(64)
        var index = 0
        for (line in log.readLines()) {
            if (line.isBlank()) continue
            val split = line.lastIndexOf(' ')
            if (split < 0) return "MALFORMED at $index"
            val body = line.substring(0, split)
            val expected = line.substring(split + 1)
            if (digest(previous, body) != expected) return "BROKEN at $index"
            previous = expected
            index++
        }
        return "OK $index lines"
    }
}
