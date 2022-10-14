package arx.core

import java.util.concurrent.atomic.AtomicLong


private data class LRUCacheEntry<V>(val value: V, var lastAccessed: Long)

open class LRUCache<K, V>(val maximumSize: Int, val targetLoadFactor: Float = 0.5f) {
    private val entries: MutableMap<K, LRUCacheEntry<V>> = mutableMapOf()
    private val incrementor = AtomicLong(0)

    fun getOrPut(k: K, f: (K) -> V): V {
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

class LRULoadingCache<K, V>(maximumSize: Int, targetLoadFactor: Float, val loadingFunction: (K) -> V) : LRUCache<K, V>(maximumSize, targetLoadFactor) {

    fun getOrPut(k: K): V {
        return getOrPut(k, loadingFunction)
    }
}


//class Grid2D<T>(defaultValue : T, initialDimensions : Vec2i = Vec2i(128,128)) {
//    private var origin = Vec2i(0,0)
//    private var min = Vec2i(0,0)
//    private var max = Vec2i(0,0)
//
//    private var capacity = initialDimensions
//    val dimensions : Vec2i get() { return dimensionsI }
//
//    private var data : Array<Any?> = arrayOfNulls(capacity.x * capacity.y)
//
//
//    private fun expandCapacity(xc: Int, yc: Int) {
//        val newData : Array<Any?> = arrayOfNulls(xc * yc)
//        for (x in 0 until capacity.x) {
//            val oldColumnIndex = x * capacity.y
//            val newColumnIndex = x * yc
//            System.arraycopy(data, oldColumnIndex, newData, newColumnIndex, capacity.y)
//        }
//    }
//
//    operator fun set(x: Int, y: Int, v: T) {
//        if (x >= dimensions.x || y >= dimensions.y) {
//            var newXC = capacity.x
//            var newYC = capacity.y
//            while (newXC <= x) {
//                newXC *= 2
//            }
//            while (newYC <= y) {
//                newYC *= 2
//            }
//
//            expandCapacity(newXC, newYC)
//        }
//    }
//
//}


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
class FiniteGrid2D<T>(val dimensions: Vec2i, private val defaultValue: T) {
    private var data: Array<Any?> = arrayOfNulls(dimensions.x * dimensions.y)

    operator fun set(x: Int, y: Int, v: T) {
        if (x < 0 || y < 0 || x >= dimensions.x || y >= dimensions.y) {
            Noto.recordError("Setting value outside of range in FiniteGrid2D")
        } else {
            data[x * dimensions.y + y] = v
        }
    }

    operator fun get(x: Int, y: Int): T {
        return if (x < 0 || y < 0 || x >= dimensions.x || y >= dimensions.y) {
            defaultValue
        } else {
            (data[x * dimensions.y + y] as T?) ?: defaultValue
        }
    }

    fun getOrUpdate(x: Int, y: Int, fn: () -> T): T {
        val raw = if (x < 0 || y < 0 || x >= dimensions.x || y >= dimensions.y) {
            null
        } else {
            (data[x * dimensions.y + y] as T?)
        }
        return if (raw == null) {
            val ret = fn()
            this[x, y] = ret
            ret
        } else {
            raw
        }
    }

    inline operator fun get(v: Vec2i): T {
        return get(v.x, v.y)
    }

}


class Watcher<T>(val fn: () -> T) {
    var previousValue: T? = null
    var first = true
    fun hasChanged(): Boolean {
        val v = fn()
        return if (first) {
            previousValue = v
            first = false
            true
        } else {
            val ret = v != previousValue
            previousValue = v
            ret
        }
    }
}


class Watcher1<C, T>(val fn: C.() -> T) {
    var previousValue: T? = null
    var first = true
    fun hasChanged(c : C): Boolean {
        val v = c.fn()
        return if (first) {
            previousValue = v
            first = false
            true
        } else {
            val ret = v != previousValue
            previousValue = v
            ret
        }
    }
}
