package arx.display.core

import arx.core.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation


@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Normalize

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Attribute(val location: Int)

class VertexArrayBuffer(stride: Int, val targetType: Int, val usage: Int, initialSize: Int) {
    var buffer : ByteBuffer = MemoryUtil.memAlloc(initialSize * stride)
    var name : Int = 0

    var openglCapacity = 0
    var lastSyncedSize = 0

    fun reserve (sizeBytes: Int) {
        while (buffer.capacity() < sizeBytes) {
            buffer = MemoryUtil.memRealloc(buffer, buffer.capacity() * 2)
            buffer.limit(buffer.capacity())
        }
    }

    fun sync(actualUsedSizeBytes: Int) {
        bind()
        glGetError()
        if (buffer.capacity() > openglCapacity) {
            buffer.position(0)
            buffer.limit(buffer.capacity())
            glBufferData(targetType, buffer, usage)
            openglCapacity = buffer.capacity()
            GL.checkError()
        } else {
            buffer.position(0)
            buffer.limit(actualUsedSizeBytes)
            glBufferSubData(targetType, 0, buffer)
            buffer.limit(buffer.capacity())
            GL.checkError()
        }
        lastSyncedSize = actualUsedSizeBytes
    }

    fun bind() {
        if (name == 0) {
            name = glGenBuffers()
        }
        GL.bindBuffer(targetType, name)
    }
}

internal data class GLAttribute(val property : KProperty<*>, val byteOffset: Int, val arity: Int, val normalize: Boolean, val openglType: Int)

internal fun glTypeToSize(type: Int) : Int {
    return when(type) {
        GL_INT -> 4
        GL_FLOAT -> 4
        GL_SHORT -> 2
        GL_BYTE -> 1
        GL_UNSIGNED_BYTE -> 1
        GL_UNSIGNED_INT -> 4
        else -> error("Unsupported opengl type: $type to compute size")
    }
}

@Suppress("NOTHING_TO_INLINE")
open class VertexDefinition {
    var buffer : VertexArrayBuffer? = null
    var currentByteOffset : Int = 0
    internal var attributes = mutableListOf<GLAttribute>()
    var totalSize: Int = 0

    init {
        val attrProps = this::class.declaredMemberProperties
            .filter { p -> p.hasAnnotation<Attribute>() }
            .sortedBy { p -> p.findAnnotation<Attribute>()!!.location  }

        for (prop in attrProps) {
            if (prop.visibility != KVisibility.PUBLIC) {
                continue
            }
            val (arity, openglType) = when (prop.returnType) {
                Vec4f::class.createType() -> Pair(4, GL_FLOAT)
                Vec3f::class.createType() -> Pair(3, GL_FLOAT)
                Vec2f::class.createType() -> Pair(2, GL_FLOAT)
                Vec4s::class.createType() -> Pair(4, GL_SHORT)
                Vec3s::class.createType() -> Pair(3, GL_SHORT)
                Vec2s::class.createType() -> Pair(2, GL_SHORT)
                Vec4i::class.createType() -> Pair(4, GL_INT)
                Vec3i::class.createType() -> Pair(3, GL_INT)
                Vec2i::class.createType() -> Pair(2, GL_INT)
                Vec4ub::class.createType() -> Pair(4, GL_UNSIGNED_BYTE)
                RGBA::class.createType() -> Pair(4, GL_UNSIGNED_BYTE)
                Vec3ub::class.createType() -> Pair(3, GL_UNSIGNED_BYTE)
                Vec2ub::class.createType() -> Pair(2, GL_UNSIGNED_BYTE)
                Float::class.createType() -> Pair(1, GL_FLOAT)
                Int::class.createType() -> Pair(1, GL_INT)
                Short::class.createType() -> Pair(1, GL_SHORT)
                VertexArrayBuffer::class.createType() -> Pair(0, 0)
                else -> error("Invalid type for Vertex: ${prop.returnType}")
            }
            if (arity > 0) {
                val propSize = arity * glTypeToSize(openglType)
                attributes.add(GLAttribute(prop, totalSize, arity, prop.hasAnnotation<Normalize>(), openglType))
                totalSize += propSize
            }
        }
    }

    inline val byteStride : Int
        get() { return totalSize }

    fun offsetFor(prop: KProperty<*>) : Int {
        for (attr in attributes) {
            if (prop == attr.property) {
                return attr.byteOffset
            }
        }
        error("Attempted to access invalid propert for vertex definition : $prop")
    }

    fun setInternal(offset: Int, v: Vec3f) {
        val tmpi = currentByteOffset + offset
        buffer!!.buffer.putFloat(tmpi, v.x)
        buffer!!.buffer.putFloat(tmpi + 4, v.y)
        buffer!!.buffer.putFloat(tmpi + 8, v.z)
    }
    fun getInternal(offset: Int, v: Vec3f) : Vec3f {
        val tmpi = currentByteOffset + offset
        v.x = buffer!!.buffer.getFloat(tmpi)
        v.y = buffer!!.buffer.getFloat(tmpi + 4)
        v.z = buffer!!.buffer.getFloat(tmpi + 8)
        return v
    }

    fun setInternal(offset: Int, v: Vec3i) {
        val tmpi = currentByteOffset + offset
        buffer!!.buffer.putInt(tmpi, v.x)
        buffer!!.buffer.putInt(tmpi + 4, v.y)
        buffer!!.buffer.putInt(tmpi + 8, v.z)
    }
    fun getInternal(offset: Int, v: Vec3i) : Vec3i {
        val tmpi = currentByteOffset + offset
        v.x = buffer!!.buffer.getInt(tmpi)
        v.y = buffer!!.buffer.getInt(tmpi + 4)
        v.z = buffer!!.buffer.getInt(tmpi + 8)
        return v
    }

    fun setInternal(offset: Int, v: Vec2f) {
        val tmpi = currentByteOffset + offset
        buffer!!.buffer.putFloat(tmpi, v.x)
        buffer!!.buffer.putFloat(tmpi + 4, v.y)
    }
    fun getInternal(offset: Int, v: Vec2f) : Vec2f {
        val tmpi = currentByteOffset + offset
        v.x = buffer!!.buffer.getFloat(tmpi)
        v.y = buffer!!.buffer.getFloat(tmpi + 4)
        return v
    }


    fun setInternal(offset: Int, v: Vec2i) {
        val tmpi = currentByteOffset + offset
        buffer!!.buffer.putInt(tmpi, v.x)
        buffer!!.buffer.putInt(tmpi + 4, v.y)
    }
    fun getInternal(offset: Int, v: Vec2i) : Vec2i {
        val tmpi = currentByteOffset + offset
        v.x = buffer!!.buffer.getInt(tmpi)
        v.y = buffer!!.buffer.getInt(tmpi + 4)
        return v
    }

    
    fun setInternal(offset: Int, v: Vec4ub) {
        val tmpi = currentByteOffset + offset
        buffer!!.buffer.put(tmpi, v.r.toByte())
        buffer!!.buffer.put(tmpi + 1, v.g.toByte())
        buffer!!.buffer.put(tmpi + 2, v.b.toByte())
        buffer!!.buffer.put(tmpi + 3, v.a.toByte())
    }
    fun getInternal(offset: Int, v: Vec4ub) : Vec4ub {
        val tmpi = currentByteOffset + offset
        v.r = buffer!!.buffer.get(tmpi).toUByte()
        v.g = buffer!!.buffer.get(tmpi + 1).toUByte()
        v.b = buffer!!.buffer.get(tmpi + 2).toUByte()
        v.a = buffer!!.buffer.get(tmpi + 3).toUByte()
        return v
    }

    fun setInternal(offset: Int, v: RGBA) {
        val tmpi = currentByteOffset + offset
        buffer!!.buffer.put(tmpi, v.r.toByte())
        buffer!!.buffer.put(tmpi + 1, v.g.toByte())
        buffer!!.buffer.put(tmpi + 2, v.b.toByte())
        buffer!!.buffer.put(tmpi + 3, v.a.toByte())
    }
    fun getInternal(offset: Int, v: RGBA) : RGBA {
        val tmpi = currentByteOffset + offset
        v.r = buffer!!.buffer.get(tmpi).toUByte()
        v.g = buffer!!.buffer.get(tmpi + 1).toUByte()
        v.b = buffer!!.buffer.get(tmpi + 2).toUByte()
        v.a = buffer!!.buffer.get(tmpi + 3).toUByte()
        return v
    }

    inline fun setInternal(offset: Int, v: Int) {
        val tmpi = currentByteOffset + offset
        buffer!!.buffer.putInt(tmpi, v)
    }
}


enum class IndexType {
    UnsignedShort,
    UnsignedInt;

    fun stride(): Int {
        return when (this) {
            UnsignedInt -> 4
            UnsignedShort -> 2
        }
    }

    fun toOpenglType() : Int {
        return when (this) {
            UnsignedInt -> GL_UNSIGNED_INT
            UnsignedShort -> GL_UNSIGNED_SHORT
        }
    }
}

/**
 * Create a Vertex Array Object representation with the given vertex layout and index type.
 * The object provided as a vertex definition is effectively transferred to the VAO and
 * will be modified internally, it should not be shared or stored elsewhere.
 */
class VAO<T : VertexDefinition>(val vertexDefinition : T, val indexType : IndexType = IndexType.UnsignedInt, usage: Int = GL_DYNAMIC_DRAW, initialCapacity: Int = 1024) {
    val maxUnsignedShort = Short.MAX_VALUE * 2
    val vertices = VertexArrayBuffer(vertexDefinition.byteStride, GL_ARRAY_BUFFER, usage, initialCapacity)
    val indices = VertexArrayBuffer(indexType.stride(), GL_ELEMENT_ARRAY_BUFFER, usage, initialCapacity + initialCapacity / 2) // a quad will have 4 vertices but 6 indexes when using triangles, (0,1,2), (2,3,0)
    var vi = 0
    var ii = 0
    var name = 0
    var modificationCounter = 0
    var lastSolidified = -1

    init {
        vertexDefinition.buffer = vertices
    }

    /**
     * Reset the buffers for writing a new set of values. Does not clear memory
     * and does not alter any GL-side information. The previously synced values
     * can still be drawn as normal.
     */
    fun reset() {
        vertexDefinition.currentByteOffset = 0
        vi = 0
        ii = 0
        modificationCounter++
    }

    /**
     * Adds a vertex to the internal buffers and returns a flyweight object capable
     * of writing to that vertex's location in memory. The returned value should
     * only be expected to be useful within the function it is called and should
     * not be stored for future use of any kind.
     */
    fun addV() : T {
        vertexDefinition.currentByteOffset = vi * vertexDefinition.byteStride
        vertices.reserve(vertexDefinition.currentByteOffset + vertexDefinition.byteStride)
        vi++
        return vertexDefinition
    }

    /**
     * Adds indexes to the internal buffer representing two triangles formed of the
     * previous 4 vertices. Should be called _after_ calls to addV()
     */
    fun addIQuad() {
        val startvi = vi-4
        if (indexType == IndexType.UnsignedShort) {
            val byteStart = ii * 2
            indices.reserve(byteStart + 12)
            if (startvi > maxUnsignedShort) { error("Invalid index size for ushort backed indices : $startvi") }
            indices.buffer.putShort(byteStart + 0, startvi.toUShort().toShort())
            indices.buffer.putShort(byteStart + 2, (startvi + 1).toUShort().toShort())
            indices.buffer.putShort(byteStart + 4, (startvi + 2).toUShort().toShort())

            indices.buffer.putShort(byteStart + 6, (startvi + 2).toUShort().toShort())
            indices.buffer.putShort(byteStart + 8, (startvi + 3).toUShort().toShort())
            indices.buffer.putShort(byteStart + 10, (startvi + 0).toUShort().toShort())
        } else {
            val byteStart = ii * 4
            indices.reserve(byteStart + 24)
            indices.buffer.putInt(byteStart + 0, startvi)
            indices.buffer.putInt(byteStart + 4, startvi + 1)
            indices.buffer.putInt(byteStart + 8, startvi + 2)

            indices.buffer.putInt(byteStart + 12, startvi + 2)
            indices.buffer.putInt(byteStart + 16, startvi + 3)
            indices.buffer.putInt(byteStart + 20, startvi + 0)
        }
        ii += 6
    }

    /**
     * Initializes the layout, name, definition, etc of the VAO
     */
    private fun initialize() {
        name = GL30.glGenVertexArrays()
        GL.bindVertexArray(name)

        indices.bind()
        vertices.bind()
        for ((index, attrib) in vertexDefinition.attributes.withIndex()) {
            GL20.glEnableVertexAttribArray(index)
            GL.checkError()
            GL20.glVertexAttribPointer(index, attrib.arity, attrib.openglType, attrib.normalize, vertexDefinition.byteStride, attrib.byteOffset.toLong())
            GL.checkError()
        }
    }

    /**
     * Sync the client side state to server side. Necessary for the changes made
     * to actually be represented when drawing. Must be called at least once prior
     * to drawing.
     */
    fun sync() {
        if (name == 0) {
            initialize()
        }
        if (modificationCounter != lastSolidified) {
            vertices.sync(vi * vertexDefinition.byteStride)
            indices.sync(ii * glTypeToSize(indexType.toOpenglType()))
            lastSolidified = modificationCounter
        }
    }

    /**
     * Draw the most recently synced data to screen
     */
    fun draw() {
        if (name == 0) { error("Must sync() VAO before attempting to draw() it") }
        GL.bindVertexArray(name)
        val indexCount = indices.lastSyncedSize / glTypeToSize(indexType.toOpenglType())
        GL11.glDrawElements(GL_TRIANGLES, indexCount, indexType.toOpenglType(), 0)
        GL.checkError()
    }
}


class MinimalVertex : VertexDefinition() {
    private val vertexOffset = offsetFor(MinimalVertex::vertex)
    private val colorOffset = offsetFor(MinimalVertex::color)
    private val texCoordOffset = offsetFor(MinimalVertex::texCoord)

    @Attribute(location = 0)
    var vertex: Vec3f
        get() {
            return getInternal(vertexOffset, Vec3f())
        }
        set(v) {
            setInternal(vertexOffset, v)
        }

    @Attribute(location = 1)
    @Normalize
    var color: RGBA
        get() {
            return getInternal(colorOffset, RGBA())
        }
        set(v) {
            setInternal(colorOffset, v)
        }

    @Attribute(location = 2)
    var texCoord: Vec2f
        get() {
            return getInternal(texCoordOffset, Vec2f())
        }
        set(v) {
            setInternal(texCoordOffset, v)
        }

    override fun toString(): String {
        return "MinimalVertex(vertex=$vertex, color=$color, texCoord=$texCoord)"
    }


}

