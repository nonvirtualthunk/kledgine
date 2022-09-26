package arx.core

import arx.core.Taxonomy.UnknownThing
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TaxonomyTest {

    @BeforeEach
    internal fun setUp() {
        System.setProperty("projectName", "test")
    }

    @Test
    fun testTaxonomyBasics() {
        assert(Taxonomy.taxon("X") == UnknownThing)
        assert(Taxonomy.taxon("NamespaceA.SubValueA") != UnknownThing)
        assert(Taxonomy.taxon("namespaceA.subvaluea") != UnknownThing)
        assert(Taxonomy.taxon("SubValueA").parents == listOf(t("valueA")))
    }
}