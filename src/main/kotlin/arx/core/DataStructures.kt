package arx.core

import java.util.concurrent.atomic.AtomicLong


private data class LRUCacheEntry<V>(val value: V, var lastAccessed : Long)

open class LRUCache<K, V>(val maximumSize: Int, val targetLoadFactor: Float = 0.5f) {
    private val entries : MutableMap<K,LRUCacheEntry<V>> = mutableMapOf()
    private val incrementor = AtomicLong(0)

    fun getOrPut (k : K, f : (K) -> V) : V {
        val entry = entries.getOrPut(k) {
            LRUCacheEntry(f(k), 0L)
        }
        entry.lastAccessed = incrementor.getAndIncrement()

        if (entries.size > maximumSize) {
            val entriesByAccess = entries.entries.sortedBy { t -> t.value.lastAccessed }
            for (i in 0 until (maximumSize.toFloat() * (1.0f - targetLoadFactor)).toInt()) {
                entries.remove(entriesByAccess[i].key)
            }
        }

        return entry.value
    }
}

class LRULoadingCache<K, V>(maximumSize: Int, targetLoadFactor: Float, val loadingFunction: (K) -> V) : LRUCache<K,V>(maximumSize, targetLoadFactor) {

    fun getOrPut(k: K) : V {
        return getOrPut(k, loadingFunction)
    }
}