package arx.game.core

import arx.core.FromConfig
import arx.core.Resources
import arx.core.Taxon
import com.typesafe.config.Config
import arx.core.*
import arx.core.Taxonomy.taxon


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


abstract class SimpleLibrary<T : FromConfig>(topLevelKey: String, confPaths: List<String>, instantiator : () -> T) : Library<T>() {

    init {
        for (path in confPaths) {
            for ((k,v) in Resources.config(path)[topLevelKey]) {
                val newV = instantiator()
                newV.readFromConfig(v)
                add(taxon("$topLevelKey.$k"), newV)
            }
        }
    }
}