package ai.obsidian.agent

import android.content.Context
import java.io.File

class Store(context: Context) {

    private val root: File = File(context.filesDir, "obsidian").apply { mkdirs() }

    fun file(name: String): File = File(root, name)

    @Synchronized
    fun append(name: String, line: String) {
        file(name).appendText("$line\n")
    }

    @Synchronized
    fun read(name: String): String {
        val target = file(name)
        return if (target.exists()) target.readText() else ""
    }

    @Synchronized
    fun tail(name: String, count: Int): String {
        val target = file(name)
        if (!target.exists()) return ""
        return target.readLines().takeLast(count).joinToString("\n")
    }
}
