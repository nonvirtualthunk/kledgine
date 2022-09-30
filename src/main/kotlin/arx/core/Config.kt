package arx.core

import arx.engine.Entity
import arx.engine.EntityData
import arx.engine.World
import arx.engine.WorldT
import com.typesafe.config.*
import io.github.config4k.*
import java.util.*


interface FromConfig {
    fun readFromConfig(cv: ConfigValue)
}

fun <T : FromConfig> T.readFromConfig(cv: ConfigValue?) {
    if (cv != null) {
        readFromConfig(cv)
    }
}

interface FromConfigCreator<T> {
    fun createFromConfig(cv: ConfigValue?): T?

    operator fun invoke(cv: ConfigValue?): T? {
        return createFromConfig(cv)
    }
}


operator fun Config?.get(path: String): ConfigValue? {
    if (this == null) {
        return null
    }

    return if (this.hasPath(path)) {
        this.getValue(path)
    } else {
        null
    }
}

operator fun ConfigValue?.get(path: String): ConfigValue? {
    if (this == null) {
        return null
    }

    return when (this.valueType()) {
        ConfigValueType.OBJECT -> (this as ConfigObject).get(path)
        else -> null
    }
}

fun ConfigValue?.asInt(): Int? {
    return when (val v = this?.unwrapped()) {
        is Int -> v
        is Double -> v.toInt()
        else -> null
    }
}

fun ConfigValue?.asFloat(): Float? {
    return when (val v = this?.unwrapped()) {
        is Int -> v.toFloat()
        is Double -> v.toFloat()
        else -> null
    }
}

fun ConfigValue?.asStr(): String? {
    return when (val v = this?.unwrapped()) {
        null -> null
        else -> v.toString()
    }
}

fun ConfigValue?.asBool(): Boolean? {
    return when (val v = this?.unwrapped()) {
        is Boolean -> v
        else -> null
    }
}

fun ConfigValue?.asList(): List<ConfigValue> {
    return when (this?.valueType()) {
        ConfigValueType.LIST -> (this as ConfigList)
        null -> emptyList()
        else -> listOf(this)
    }
}

fun ConfigValue?.asTaxonList(): List<Taxon> {
    return this.asList().mapNotNull { it.asStr() }.map { t(it) }
}

fun ConfigValue?.isList(): Boolean {
    return when (this?.valueType()) {
        ConfigValueType.LIST -> true
        else -> false
    }
}

fun ConfigValue?.forEach(f: (String, ConfigValue) -> Unit) {
    if (this.isObject()) {
        (this as ConfigObject).forEach(f)
    }
}

fun <K, V> ConfigValue?.map(f: (String, ConfigValue) -> Pair<K, V>?): Map<K, V> {
    return if (this == null) {
        mapOf()
    } else if (this.isObject()) {
        val ret = mutableMapOf<K, V>()
        (this as ConfigObject).forEach { k, v ->
            val mapped = f(k, v)
            if (mapped != null) {
                ret[mapped.first] = mapped.second
            }
        }
        ret
    } else {
        mapOf()
    }
}

fun ConfigValue?.isObject(): Boolean {
    return when (this?.valueType()) {
        ConfigValueType.OBJECT -> true
        else -> false
    }
}

fun ConfigValue?.isStr(): Boolean {
    return this?.valueType() == ConfigValueType.STRING
}

fun ConfigValue?.isInt(): Boolean {
    return this?.valueType() == ConfigValueType.NUMBER
}

fun ConfigValue?.isNum(): Boolean {
    return this?.valueType() == ConfigValueType.NUMBER
}

fun ConfigValue?.isBool(): Boolean {
    return this?.valueType() == ConfigValueType.BOOLEAN
}

operator fun ConfigValue?.iterator(): Iterator<Map.Entry<String, ConfigValue>> {
    return if (isObject()) {
        (this as Map<String, ConfigValue>).iterator()
    } else {
        Collections.emptyIterator()
    }
}


inline fun <reified T> registerCustomTypeRender() {
    registerCustomType(object : CustomType {
        override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
            TODO("Not yet implemented")
        }

        override fun testParse(clazz: ClassContainer): Boolean {
            return false
        }

        override fun testToConfig(obj: Any): Boolean {
            return obj is T
        }

        override fun toConfig(obj: Any, name: String): Config {
            return ConfigFactory.parseMap(mapOf(name to obj.toString()))
        }
    })
}

object ConfigRegistration {
    var activeWorld: WorldT<EntityData>? = null

    init {
        registerCustomType(object : CustomType {
            override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
                TODO("Not yet implemented")
            }

            override fun testParse(clazz: ClassContainer): Boolean {
                return false
            }

            override fun testToConfig(obj: Any): Boolean {
                if (obj is Map<*, *>) {
                    if (obj.isNotEmpty()) {
                        return true
                    }
                }
                return false
            }

            fun Any.toConfigValue(): ConfigValue {
                val name = "dummy"
                return this.toConfig(name).root()[name]!!
            }


            override fun toConfig(obj: Any, name: String): Config {
                val thisMap = obj as Map<*, *>

                val map = thisMap
                    .mapKeys {
                        if (it.key is Entity) {
                            (it.key as Entity).toString(activeWorld)
                        } else {
                            it.key.toString()
                        }
                    }
                    .mapValues { it.value?.toConfigValue()?.unwrapped() }
                return ConfigFactory.parseMap(mapOf(name to map))
            }
        })
    }
}