package arx.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BindingContextTest {


    data class Foo(val i : Int, val m : Map<String, Float>)

    @Test
    internal fun testNestedResolution() {
        val bindings = mapOf("f" to Foo(3, mapOf("hello" to 0.5f)))

        val ctx = BindingContext(bindings, null)

        assertEquals(0.5f, ctx.resolve("f.m.hello"))
        assertEquals(3, ctx.resolve("f.i"))
    }
}