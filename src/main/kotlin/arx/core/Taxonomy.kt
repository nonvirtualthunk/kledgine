package arx.core

import arx.core.Noto.err
import arx.core.Noto.warn
import com.typesafe.config.ConfigValue
import java.lang.Integer.min
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private val TaxonIDCounter = AtomicInteger(0)


interface Identifiable {
    var identity: Taxon
}

data class Taxon(val namespace : String, val name: String, val parents : List<Taxon> = emptyList()) {
    val id = TaxonIDCounter.incrementAndGet()
    val normalizedNamespace = Taxonomy.normalizeString(namespace)
    val namespaceSegments = if (normalizedNamespace.isEmpty()) { emptyList() } else { normalizedNamespace.split('.') }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return id
    }
}

data class ProtoTaxon(val namespace : String, val name: String, val parentIdentifiers : List<String> = emptyList())


private fun concatNamespaces(a: String, b: String) : String {
    return if (a.isEmpty()) {
        b
    } else {
        "$a.$b"
    }
}

private fun extractProtoTaxons(conf: ConfigValue, namespaceBase: String, isNestedTaxonomy: Boolean, defaultParents : List<String>, out : MutableList<ProtoTaxon>) {
    conf.forEach { k, v ->
        if (v.isObject()) {
            if (isNestedTaxonomy) {
                extractProtoTaxons(v, concatNamespaces(namespaceBase, k), isNestedTaxonomy, defaultParents, out)
            } else {
                val parents = v["isA"].asList().mapNotNull { it.asStr() }
                out.add(ProtoTaxon(namespaceBase, k, defaultParents + parents))
            }
        } else if (v.isList()) {
            out.add(ProtoTaxon(namespaceBase, k, defaultParents + v.asList().mapNotNull { it.asStr() }))
        } else {
            val parentStr = v.asStr()
            if (parentStr != null) {
                out.add(ProtoTaxon(namespaceBase, k, defaultParents + listOf(parentStr)))
            } else {
                Noto.recordError("Invalid type for taxon parent", mapOf("value" to v, "name" to k, "namespace" to namespaceBase))
            }
        }
    }
}

fun t(str: String) : Taxon {
    return Taxonomy.taxon(str)
}

object Taxonomy {
    val RootNamespace = ""

    val UnknownThing = Taxon(RootNamespace, "UnknownThing")




    val taxonsByName = TreeMap<String, List<Taxon>>(java.lang.String.CASE_INSENSITIVE_ORDER)
    val taxonsByAllPossibleIdentifiers = TreeMap<String, List<Taxon>>(java.lang.String.CASE_INSENSITIVE_ORDER)
    var loaded = false


    fun normalizeString(str: String) : String {
        return str.lowercase()
    }

    fun taxon(str : String) : Taxon {
        val ts = taxonsByAllPossibleIdentifiers[str]
        return if (ts == null) {
            UnknownThing
        } else if (ts.size == 1) {
            ts[0]
        } else {
            warn("Taxon resolution with multiple possible results, choosing arbitrarily among $ts")
            ts[0]
        }
    }

    fun load() {
        if (loaded) {
            return
        }
        loaded = true

        val projectName = System.getProperty("projectName")
        assert(projectName != null)


        val conf = Resources.config("$projectName/taxonomy/taxonomy.sml")

        val allTaxons = mutableListOf<ProtoTaxon>()
        conf["Taxonomy"]?.let { extractProtoTaxons(it, "", isNestedTaxonomy = true, emptyList(), allTaxons) }

        for (src in conf["TaxonomySources"].asList()) {
            val sections = src.asList()
            if (sections.isEmpty()) { err("Empty list in taxonomy sources? $src") }
            val srcConf = Resources.config("$projectName/${sections[0].asStr()}")
            srcConf.root().forEach { k,v ->
                if (sections.size == 1 || sections.any { s -> s.asStr() == k }) {
                    val parentName = if (k.endsWith("ies")) {
                        k.substring(0 until k.length - 3) + "y"
                    } else if (k.endsWith("es")) {
                        k.substring(0 until k.length - 2)
                    } else {
                        k.substring(0 until k.length - 1)
                    }
                    allTaxons.add(ProtoTaxon(RootNamespace, parentName))
                    extractProtoTaxons(v, k, isNestedTaxonomy = false, listOf(parentName), allTaxons)
                }
            }
        }


        val protoTaxonsByName = TreeMap<String, List<ProtoTaxon>>(java.lang.String.CASE_INSENSITIVE_ORDER)
        for (t in allTaxons) {
            protoTaxonsByName[t.name] = (protoTaxonsByName[t.name] ?: emptyList()) + t
        }

        fun findTaxon(pt: ProtoTaxon) : Taxon? {
            val l : List<Taxon> = (taxonsByName[pt.name] ?: emptyList())
            return l.find { t -> t.normalizedNamespace == normalizeString(pt.namespace) }
        }

        fun namespaceSimilarity(a : String, b : String) : Int {
            val aSections = a.split('.')
            val bSections = b.split('.')
            var matchCount = 0
            for (i in 0 until min(aSections.size, bSections.size)) {
                if (normalizeString(aSections[i]) == normalizeString(bSections[i])) {
                    matchCount++
                } else {
                    break
                }
            }
            return matchCount
        }

        fun findProtoTaxonByIdentifier(str: String, baseNamespace: String): ProtoTaxon? {
            val lastDotIndex = str.lastIndexOf('.')
            val (namespace, name) = if (lastDotIndex == -1) {
                null to normalizeString(str)
            } else {
                normalizeString(str.substring(0, lastDotIndex)) to str.substring(lastDotIndex + 1, str.length)
            }

            val possibleMatches = protoTaxonsByName[name]?.filter { pt -> namespace == null || normalizeString(pt.namespace) == namespace } ?: emptyList()
            return if (possibleMatches.size > 1) {
                possibleMatches.maxBy { ppt -> namespaceSimilarity(ppt.namespace, baseNamespace) }
            } else if (possibleMatches.isNotEmpty()) {
                possibleMatches[0]
            } else {
                null
            }
        }

        fun processProtoTaxon(pt: ProtoTaxon) : Taxon {
            val existing = findTaxon(pt)
            return if (existing == null) {
                var parentTaxons : List<Taxon> = emptyList()
                for (pi in pt.parentIdentifiers) {
                    val parentPT = findProtoTaxonByIdentifier(pi, pt.namespace)
                    if (parentPT == null) {
                        err("Taxon with unresolveable parent $pi")
                    } else {
                        parentTaxons = parentTaxons + processProtoTaxon(parentPT)
                    }
                }

                val newTaxon = Taxon(pt.namespace, pt.name, parentTaxons)
                var accumIdentifier = newTaxon.name
                for (s in newTaxon.namespaceSegments.reversed()) {
                    accumIdentifier = "$s.$accumIdentifier"
                    taxonsByAllPossibleIdentifiers[accumIdentifier] = (taxonsByAllPossibleIdentifiers[accumIdentifier] ?: emptyList()) + newTaxon
                }
                taxonsByName[newTaxon.name] = (taxonsByName[newTaxon.name] ?: emptyList()) + newTaxon
                taxonsByAllPossibleIdentifiers[newTaxon.name] = (taxonsByAllPossibleIdentifiers[newTaxon.name] ?: emptyList()) + newTaxon
                newTaxon
            } else {
                existing
            }
        }

        for (t in allTaxons) {
            processProtoTaxon(t)
        }
    }

    init {
        load()
    }

}

fun main() {
    Taxonomy.taxon("X")
}


