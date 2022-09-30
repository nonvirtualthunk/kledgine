package arx.game.core

import arx.core.FromConfig
import arx.core.Resources
import arx.core.Taxon
import com.typesafe.config.Config
import arx.core.*
import arx.core.Taxonomy.taxon
import com.typesafe.config.ConfigValue


@JvmInline
value class LibraryKey(val i: Int) {

}

abstract class Library<T> {
    protected val values = mutableListOf<T>()
    protected val valuesByTaxon = mutableMapOf<Taxon, T>()


    protected fun add(k: Taxon, v: T) {
        values.add(v)
        valuesByTaxon[k] = v
        if (v is Identifiable) {
            v.identity = k
        }
    }

    operator fun get(t : Taxon) : T? {
        return valuesByTaxon[t]
    }

    operator fun iterator() : Iterator<Map.Entry<Taxon, T>> {
        return valuesByTaxon.iterator()
    }
}


abstract class SimpleLibrary<T>(topLevelKey: String, confPaths: List<String>, instantiator : (ConfigValue) -> T?) : Library<T>() {

    init {
        for (path in confPaths) {
            for ((k,v) in Resources.config(path)[topLevelKey]) {
                instantiator(v)?.let { newV ->
                    add(taxon("$topLevelKey.$k"), newV)
                }
            }
        }
    }
}