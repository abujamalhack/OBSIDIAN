package ai.obsidian.agent.root

import android.content.Context
import java.io.BufferedReader

object Shell {

    @Volatile
    private var rootState: Boolean? = null

    fun run(context: Context, command: String): String {
        val rooted = rootState ?: probe()
        return if (rooted) execute(arrayOf("su", "-c", command)) else execute(arrayOf("sh", "-c", command))
    }

    fun rootAvailable(): Boolean = rootState ?: probe()

    @Synchronized
    private fun probe(): Boolean {
        rootState?.let { return it }
        val result = runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = BufferedReader(process.inputStream.reader()).readText()
            process.waitFor()
            output.contains("uid=0")
        }.getOrDefault(false)
        rootState = result
        return result
    }

    private fun execute(command: Array<String>): String = runCatching {
        val process = Runtime.getRuntime().exec(command)
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        process.waitFor()
        (stdout + stderr).ifBlank { "(no output)" }.take(MAX_OUTPUT)
    }.getOrElse { "exec failed: ${it.message}" }

    private const val MAX_OUTPUT = 60_000
}
