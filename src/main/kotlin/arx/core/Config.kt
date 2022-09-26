package arx.core

import com.typesafe.config.*
import io.github.config4k.extract
import java.util.*


interface FromConfig {
    fun readFromConfig(cv: ConfigValue)
}

interface FromConfigCreator<T> {
    fun createFromConfig(cv: ConfigValue?) : T
}


operator fun Config?.get(path: String) : ConfigValue? {
    if (this == null) { return null }

    return if (this.hasPath(path)) {
        this.getValue(path)
    } else {
        null
    }
}

operator fun ConfigValue?.get(path: String) : ConfigValue? {
    if (this == null) { return null }

    return when (this.valueType()) {
        ConfigValueType.OBJECT -> (this as ConfigObject).get(path)
        else -> null
    }
}

fun ConfigValue?.asInt() : Int? {
    return when (val v = this?.unwrapped()) {
        is Int -> v
        is Double -> v.toInt()
        else -> null
    }
}

fun ConfigValue?.asFloat() : Float? {
    return when (val v = this?.unwrapped()) {
        is Int -> v.toFloat()
        is Double -> v.toFloat()
        else -> null
    }
}

fun ConfigValue?.asStr() : String? {
    return when (val v = this?.unwrapped()) {
        null -> null
        else -> v.toString()
    }
}

fun ConfigValue?.asBool(): Boolean? {
    return when(val v = this?.unwrapped()) {
        is Boolean -> v
        else -> null
    }
}

fun ConfigValue?.asList() : List<ConfigValue> {
    return when (this?.valueType()) {
        ConfigValueType.LIST -> (this as ConfigList)
        null -> emptyList()
        else -> listOf(this)
    }
}

fun ConfigValue?.isList() : Boolean {
    return when (this?.valueType()) {
        ConfigValueType.LIST -> true
        else -> false
    }
}

fun ConfigValue?.forEach(f : (String, ConfigValue) -> Unit) {
    if (this.isObject()) {
        (this as ConfigObject).forEach(f)
    }
}

fun ConfigValue?.isObject(): Boolean {
    return when (this?.valueType()) {
        ConfigValueType.OBJECT -> true
        else -> false
    }
}

operator fun ConfigValue?.iterator() : Iterator<Map.Entry<String, ConfigValue>> {
    return if (isObject()) {
        (this as Map<String, ConfigValue>).iterator()
    } else {
        Collections.emptyIterator()
    }
}