package ai.obsidian.agent.net

object Dedupe {

    private const val CAPACITY = 256

    private val seen = object : LinkedHashMap<String, Boolean>(CAPACITY, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>): Boolean =
            size > CAPACITY
    }

    @Synchronized
    fun firstSighting(id: String): Boolean = seen.put(id, true) == null
}
