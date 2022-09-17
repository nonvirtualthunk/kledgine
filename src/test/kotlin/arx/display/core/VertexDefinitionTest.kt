package arx.display.core

import arx.core.Vec3f
import arx.core.Vec4ub
import org.junit.jupiter.api.Assertions.*
import org.lwjgl.opengl.GL15

internal class VertexDefinitionTest {
    class FooVertex : VertexDefinition() {
        private val positionOffset = offsetFor(FooVertex::position)
        private val colorOffset = offsetFor(FooVertex::color)
        private val numOffset = offsetFor(FooVertex::num)

        @Attribute(location = 0)
        var position : Vec3f
            get() { TODO() }
            set(v) { setInternal(positionOffset, v) }

        @Normalize
        @Attribute(location = 1)
        var color : Vec4ub
            get() { TODO() }
            set(v) { setInternal(colorOffset, v) }

        @Attribute(location = 2)
        var num : Int
            get() { TODO() }
            set(v) { setInternal(numOffset, v) }


    }

    fun testVertexDefinitionLayout() {
        val v = FooVertex()

        val buff = VertexArrayBuffer(v.byteStride, GL15.GL_ARRAY_BUFFER, GL15.GL_DYNAMIC_DRAW, 1)

        for (i in 0 until v.byteStride) {
            buff.buffer.put(i, 0)
        }

        v.buffer = buff

        v.color = Vec4ub(0u,128u,255u,0u)
        v.position = Vec3f(19.0f, 22.0f, 33.0f)

        println("Bytes")
        print("[")
        for (i in 0 until v.byteStride) {
            print("${buff.buffer.get(i).toUByte()},")
        }
        println("]")

        println("As floats")
        print("[")
        for (i in 0 until v.byteStride / 4) {
            print("${buff.buffer.getFloat(i*4)},")
        }
        println("]")
    }
}