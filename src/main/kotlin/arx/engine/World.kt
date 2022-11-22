package arx.engine

import arx.core.*
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import io.github.config4k.ClassContainer
import io.github.config4k.CustomType
import io.github.config4k.registerCustomType
import io.github.config4k.toConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.reflect.KClass

val DataTypeIncrementor = AtomicInteger()

val DataTypesByClass = ConcurrentHashMap<KClass<*>, DataType<*>>()


typealias EntityId = Int

interface EntityWrapper {
    val entity : Entity
}

interface EntityWithData<T> {
    val entity : Entity
}

@JvmInline
value class Entity(val id: EntityId) {
    companion object {
        init {
            registerCustomType(object : CustomType {
                override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
                    TODO("Not yet implemented")
                }

                override fun testParse(clazz: ClassContainer): Boolean {
                    return false
                }

                override fun testToConfig(obj: Any): Boolean {
                    return obj is Entity
                }

                override fun toConfig(obj: Any, name: String): Config {
                    val ent = obj as Entity

                    return ConfigFactory.parseMap(mapOf(name to ent.toString(ConfigRegistration.activeWorld)))
                }
            })
        }
    }

    override fun toString(): String {
        return "Entity($id)"
    }

    fun <T : EntityData>toString(world: WorldT<T>?) : String {
        return if (world != null) {
            with(world) {
                this@Entity[Identity as DataType<T>].ifLet {
                    val ident = it as Identity
                    ident.name ?: "${ident.identity}(${this@Entity.id})"
                }.orElse {
                    this@Entity.toString()
                }
            }
        } else {
            this@Entity.toString()
        }
    }
}

fun GameWorld.prettyString(e : Entity) : String {
    return e.toString(this)
}

interface EntityData {
    fun dataType() : DataType<*>
}

interface CreateOnAccessData

interface GameData : EntityData {

}

interface DisplayData : EntityData {

}

@Suppress("LeakingThis")
open class DataType<out T>(val creator: () -> T, val versioned: Boolean = false, val sparse: Boolean = false) {
    val defaultInstance = creator()
    constructor (di : T, versioned: Boolean = false, sparse : Boolean = false) : this({di}, versioned, sparse)

    val index = DataTypeIncrementor.getAndIncrement()
    init {
        DataTypesByClass[defaultInstance!!::class] = this
    }

    override fun toString(): String {
        return defaultInstance!!::class.simpleName!!
    }
}

open class DisplayDataType<T>(defaultInst: () -> T, versioned: Boolean, sparse : Boolean) : DataType<T>(defaultInst, versioned, sparse) {
    constructor (di : T, versioned: Boolean = false, sparse : Boolean = false) : this({di}, versioned, sparse)
}

interface DataContainer {
    val dataType : DataType<*>
    companion object {
        const val LatestVersion = Long.MAX_VALUE
    }

    fun <T>value(e: Int, version: Long): T?
    fun <T>setValue(e: Int, version: Long, value: T)
    fun advanceMinVersion(version: Long)
    fun resize(size: Int) {}

    fun idsWithData() : Iterator<Int>
    fun remove(e: Int)
}

@Suppress("UNCHECKED_CAST")
class UnversionedMapDataContainer(override val dataType : DataType<*>) : DataContainer {
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

    override fun remove(e: Int) {
        values.remove(e)
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
class SingleThreadArrayDataContainer(override val dataType : DataType<*>, initialSize : Int) : DataContainer {
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

    override fun remove(e: Int) {
        values[e] = null
    }
}

@Suppress("UNCHECKED_CAST")
class VersionedArrayDataContainer(override val dataType : DataType<*>, initialSize: Int) : DataContainer {
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

    override fun remove(e: Int) {
        values[e] = null
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
    val destroyedEntities = mutableListOf<Entity>()
    var globalEntity = createEntity()

    var eventCallbacks = listOf<(Event) -> Unit>()

    fun fireEvent(event : Event) {
        eventCallbacks.forEach { fn -> fn(event)}
    }

    fun createEntity() : Entity {
        val ret = if (destroyedEntities.isNotEmpty()) {
            destroyedEntities.pop()
        } else {
            Entity(entityCounter.getAndIncrement())
        }
        entities.add(ret)

        if (entityCapacity <= ret.id) {
            entityCapacity *= 2
            for (dc in dataContainers) {
                dc?.resize(entityCapacity)
            }
        }

        return ret
    }
    fun destroyEntity(ent : Entity) {
        entities.remove(ent)
        for (dc in dataContainers) {
            dc?.remove(ent.id)
        }
        destroyedEntities.add(ent)
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

    @JvmName("get1")
    @Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
    operator fun <T : B> get(dt: DataType<T>) : T where T : CreateOnAccessData {
        return globalEntity[dt]
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
            if (dataContainers[dt.index] != null) { return }

            if (dt.versioned) {
                dataContainers[dt.index] = VersionedArrayDataContainer(dt, entityCapacity)
            } else {
                if (dt.sparse) {
                    dataContainers[dt.index] = UnversionedMapDataContainer(dt)
                } else {
                    dataContainers[dt.index] = SingleThreadArrayDataContainer(dt, entityCapacity)
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

    @JvmName("get1")
    @Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
    operator fun <T> Entity.get(dt: DataType<T>) : T where T : B, T : CreateOnAccessData {
        val raw = this@WorldT.data(this, dt)
        return if (raw == null) {
            val new = dt.creator()
            attachData(new)
            new
        } else {
            raw
        }
    }


    operator fun <T> EntityWithData<T>.get(dt: DataType<T>) : T where T : B {
        return this@WorldT.data(this.entity, dt)!!
    }
    operator fun <T> Entity.get(dt: DataType<T>) : T? where T : B {
        return this@WorldT.data(this, dt)
    }

    operator fun <T> EntityWrapper.get(dt: DataType<T>) : T? where T : B {
        return this@WorldT.data(this.entity, dt)
    }

    fun <T : B> Entity.attachData(t: T) {
        return this@WorldT.attachData(this, t)
    }

    fun Entity.printAllData() {
        ConfigRegistration.activeWorld = this@WorldT as WorldT<EntityData>
        val renderOptions = ConfigRenderOptions.concise().setFormatted(true).setComments(false).setOriginComments(false)
        println("/==============================================================\\")
        println(this.toString(this@WorldT))
        println()
        for (dc in dataContainers) {
            dc?.value<Any>(this.id, DataContainer.LatestVersion)?.let {
                val typeStr = dc.dataType.toString()
                println(typeStr + " " + it.toConfig(typeStr).root()[typeStr]?.render(renderOptions))
            }
        }
        println("\\==============================================================/")
    }

    operator fun <T> InitWithWorldT<T, B>.invoke() : T {
        return this.invoke(this@WorldT)
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


data class InitWithWorldT<T, out B : EntityData>(val fn : WorldT<B>.() -> T) {
    private var acquired : T? = null
    operator fun invoke(w: WorldT<B>) : T {
        if (acquired == null) {
            acquired = fn(w)
        }
        return acquired!!
    }
}

fun <T> initWithWorld(fn : World.() -> T) : InitWithWorldT<T, EntityData> {
    return InitWithWorldT(fn)
}