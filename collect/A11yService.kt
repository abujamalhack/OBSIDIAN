package ai.obsidian.agent.collect

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class A11yService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentWindow = event.packageName?.toString().orEmpty()
        }
    }

    override fun onInterrupt() = Unit

    fun scrape(): String {
        val root = rootInActiveWindow ?: return "no active window"
        val builder = StringBuilder()
        walk(root, "", builder, 0)
        return builder.toString().ifEmpty { "empty tree" }
    }

    fun click(path: String): Boolean {
        val root = rootInActiveWindow ?: return false
        var node: AccessibilityNodeInfo = root
        for (segment in path.split('/').filter { it.isNotEmpty() }) {
            val index = segment.toIntOrNull() ?: return false
            node = node.getChild(index) ?: return false
        }
        val clickable = generateSequence(node) { it.parent }
            .firstOrNull { it.isClickable } ?: return false
        return clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun walk(
        node: AccessibilityNodeInfo,
        path: String,
        builder: StringBuilder,
        depth: Int
    ) {
        if (depth > MAX_DEPTH) return
        val label = node.text?.toString() ?: node.contentDescription?.toString()
        if (!label.isNullOrBlank()) {
            builder.append(path)
                .append(" [")
                .append(node.className?.substringAfterLast('.'))
                .append("] ")
                .append(label)
                .append('\n')
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            walk(child, "$path$index/", builder, depth + 1)
        }
    }

    companion object {
        private const val MAX_DEPTH = 24

        @Volatile
        var instance: A11yService? = null

        @Volatile
        var currentWindow: String = ""
    }
}
