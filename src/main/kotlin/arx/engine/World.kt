package arx.engine

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.reflect.KClass

val DataTypeIncrementor = AtomicInteger()

val DataTypesByClass = ConcurrentHashMap<KClass<*>, DataType<*>>()


typealias EntityId = Int

@JvmInline
value class Entity(val id: EntityId)

interface EntityData {
    fun dataType() : DataType<*>
}

interface GameData : EntityData {

}

interface DisplayData : EntityData {

}

@Suppress("LeakingThis")
open class DataType<T>(val defaultInstance: T, val versioned: Boolean = false, val sparse: Boolean = false) {
    val index = DataTypeIncrementor.getAndIncrement()
    init {
        DataTypesByClass[defaultInstance!!::class] = this
    }
}

open class DisplayDataType<T>(defaultInst: T, versioned: Boolean) : DataType<T>(defaultInst, versioned)

interface DataContainer {
    companion object {
        const val LatestVersion = Long.MAX_VALUE
    }

    fun <T>value(e: Int, version: Long): T?
    fun <T>setValue(e: Int, version: Long, value: T)
    fun advanceMinVersion(version: Long)
    fun resize(size: Int) {}

    fun idsWithData() : Iterator<Int>
}

@Suppress("UNCHECKED_CAST")
class UnversionedMapDataContainer : DataContainer {
    private val values : ConcurrentHashMap<Int, Any> = ConcurrentHashMap()

    override fun <T> value(e: Int, version: Long): T? {
        return values[e] as T?
    }

    override fun <T> setValue(e: Int, version: Long, value: T) {
        values[e] = value as Any
    }

    override fun resize(size: Int) {
        // no-op
    }

    override fun advanceMinVersion(version: Long) {
        // no-op
    }

    override fun idsWithData(): Iterator<Int> {
        return values.keys().iterator()
    }
}

//@Suppress("UNCHECKED_CAST")
//class ArrayDataContainer(initialSize : Int) : DataContainer {
//    @Volatile var values: AtomicReferenceArray<Any?> = AtomicReferenceArray(initialSize)
//    @Volatile var secondaryWriter: AtomicReferenceArray<Any?>? = null
//
//    override fun <U>value(e: Int, version: Long): U? = values[e] as U?
//
//    override fun <T> setValue(e: Int, version: Long, value: T) {
//        values.set(e, value)
//    }
//
//    override fun resize(size: Int) {
//        val newValues: AtomicReferenceArray<Any?> = AtomicReferenceArray(size)
//        secondaryWriter = newValues
//        for (i in 0 until values.length()) {
//            newValues[i] = values[i]
//        }
//        values = newValues
//        secondaryWriter = null
//    }
//}

@Suppress("UNCHECKED_CAST")
class SingleThreadArrayDataContainer(initialSize : Int) : DataContainer {
    var values : Array<Any?> = arrayOfNulls(initialSize)

    override fun <T> value(e: Int, version: Long): T? {
        return values[e] as T?
    }

    override fun <T> setValue(e: Int, version: Long, value: T) {
        values[e] = value
    }

    override fun resize(size: Int) {
        values = values.copyOf(size)
    }

    override fun advanceMinVersion(version: Long) {
        // no-op
    }

    override fun idsWithData(): Iterator<Int> {
        return iterator {
            for (i in values.indices) {
                if (values[i] != null) {
                    yield(i)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class VersionedArrayDataContainer(initialSize: Int) : DataContainer {
    var values : Array<VersionedContainer?> = arrayOfNulls(initialSize)
    var minVersion: Long = 0L

    class VersionedContainer {
        var values : Array<Any?> = arrayOfNulls(4)
        var versions : LongArray = LongArray(4)
        var offset : Int = 0
        var size : Int = 0
    }

    override fun <T> value(e: Int, version: Long): T? {
        val cur = values[e]
        if (cur == null || cur.size == 0) {
            return null
        } else {
            if (version == DataContainer.LatestVersion) {
                return cur.values[(cur.offset + cur.size - 1) and (cur.values.size - 1)] as T?
            }

            var ret : T? = cur.values[cur.offset] as T?
            for (d in 1 until cur.size) {
                val i = (cur.offset + d) and (cur.values.size - 1)
                if (cur.versions[i] > version) { break }
                ret = cur.values[i] as T?
            }
            return ret
        }
    }

    override fun <T> setValue(e: Int, version: Long, value: T) {
        var cur = values[e]
        if (cur == null) {
            cur = VersionedContainer()
            values[e] = cur
        }

        while (cur.size > 0 && cur.versions[cur.offset] < minVersion) {
            cur.offset = (cur.offset + 1) and (cur.values.size - 1)
            cur.size--
        }

        if (cur.size == cur.versions.size) {
            val newValues = arrayOfNulls<Any?>(cur.size * 2)
            val newVersions = LongArray(cur.size * 2)

            for (d in 0 until cur.size) {
                val i = (cur.offset + d) and (cur.values.size - 1)
                newValues[d] = cur.values[i]
                newVersions[d] = cur.versions[i]
            }

            cur.values = newValues
            cur.versions = newVersions
            cur.offset = 0
        }

        // versions must be monotonically >= and >= the min version, so enforce that here
        var effVersion = max(minVersion, version)
        if (cur.size > 0) {
            effVersion = max(effVersion, cur.versions[cur.offset + cur.size - 1])
        }
        // actually set the new versioned values
        val ni = (cur.offset + cur.size) and (cur.values.size - 1)
        cur.values[ni] = value
        cur.versions[ni] = effVersion
        cur.size++
    }

    override fun resize(size: Int) {
        values = values.copyOf(size)
    }

    override fun advanceMinVersion(version: Long) {
        minVersion = max(version, minVersion)
    }

    override fun idsWithData(): Iterator<Int> {
        return iterator {
            for (i in values.indices) {
                if (values[i] != null) {
                    yield(i)
                }
            }
        }
    }
}


interface WorldViewT<in B> {
    operator fun <T : B>get (dt: DataType<T>, entity: Entity) : T? {
        return data(entity, dt)
    }

    fun <T : B>data (entity: Entity, dt: DataType<T>) : T?

    fun <T : B>data (entity: Entity, dt: DataType<T>, version: Long) : T?

    fun <T : B>global (dt: DataType<T>) : T?

    fun <T : B>global (dt: DataType<T>, version: Long) : T?
}

@Suppress("NOTHING_TO_INLINE")
class WorldT<in B : EntityData> : WorldViewT<B> {
    val dataContainers = arrayOfNulls<DataContainer?>(64)
    val dataTypes = arrayOfNulls<DataType<*>?>(64)
    var entities = mutableSetOf<Entity>()
    var entityCounter = AtomicInteger()
    var entityCapacity = 2048
    var globalEntity = createEntity()


    fun createEntity() : Entity {
        val ret = Entity(entityCounter.getAndIncrement())
        entities.add(ret)

        if (entityCapacity <= ret.id) {
            entityCapacity *= 2
            for (dc in dataContainers) {
                dc?.resize(entityCapacity)
            }
        }

        return ret
    }

    inline fun <T : B> dataContainer(dt : DataType<T>) : DataContainer {
        val dc = dataContainers[dt.index]
        return if (dc == null) {
            register(dt)
            dataContainers[dt.index]!!
        } else {
            dc
        }
    }

    fun <T : B>entitiesWithData(dt: DataType<T>) : Iterator<Entity> {
        return iterator {
            val dc = dataContainer(dt)
            for (id in dc.idsWithData()) {
                yield(Entity(id))
            }
        }
    }

    override fun <T : B>data (entity: Entity, dt: DataType<T>, version: Long) : T? {
        return dataContainer(dt).value<T>(entity.id, version)
    }

    override fun <T : B>data (entity: Entity, dt: DataType<T>) : T? {
        return dataContainer(dt).value<T>(entity.id, DataContainer.LatestVersion)
    }

    override fun <T : B> global(dt: DataType<T>): T? {
        return data(globalEntity, dt)
    }

    override fun <T : B> global(dt: DataType<T>, version: Long): T? {
        return data(globalEntity, dt, version)
    }

    operator fun <T : B> get(dt: DataType<T>) : T? {
        return global(dt)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : B>attachData (entity: Entity, data: T, version: Long = 0L) {
        dataContainer(data.dataType() as DataType<T>).setValue(entity.id, version, data)
    }

    fun <T : B>attachData (data: T, version: Long = 0L) {
        attachData(globalEntity, data, version)
    }

    fun <T : B>register(dt : DataType<T>) {
        synchronized(this) {
            if (dt.versioned) {
                dataContainers[dt.index] = VersionedArrayDataContainer(entityCapacity)
            } else {
                if (dt.sparse) {
                    dataContainers[dt.index] = UnversionedMapDataContainer()
                } else {
                    dataContainers[dt.index] = SingleThreadArrayDataContainer(entityCapacity)
                }
            }
            dataTypes[dt.index] = dt
        }
    }

    fun advanceMinVersion(v: Long) {
        for (dc in dataContainers) {
            dc?.advanceMinVersion(v)
        }
    }

    operator fun <T : B> Entity.get(dt: DataType<T>) : T? {
        return this@WorldT.data(this, dt)
    }

    fun <T : B> Entity.attachData(t: T) {
        return this@WorldT.attachData(this, t)
    }
}

typealias GameWorld = WorldT<GameData>

typealias World = WorldT<EntityData>


typealias GameWorldView = WorldViewT<GameData>

typealias WorldView = WorldViewT<EntityData>


class VersionedWorldViewT<in B : EntityData>(private val world: WorldT<B>, private val version: Long) : WorldViewT<B> {
    override fun <T : B> data(entity: Entity, dt: DataType<T>): T? {
        return world.data(entity, dt, version)
    }

    override fun <T : B> data(entity: Entity, dt: DataType<T>, version: Long): T? {
        return world.data(entity, dt, version)
    }

    override fun <T : B> global(dt: DataType<T>): T? {
        return world.global(dt, version)
    }

    override fun <T : B> global(dt: DataType<T>, version: Long): T? {
        return world.global(dt, version)
    }
}

typealias VersionedGameWorldView = VersionedWorldViewT<EntityData>
typealias VersionedWorldView = VersionedWorldViewT<DisplayData>