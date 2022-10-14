package arx.core

import arx.core.Axis
import arx.core.Axis2D

open class Vec2f(var elem0: Float = 0.toFloat(), var elem1: Float = 0.toFloat()) {
    companion object {
        init {
            registerCustomTypeRender<Vec2f>()
        }
    }

    operator fun invoke(arg0: Float, arg1: Float) {
        elem0 = arg0
        elem1 = arg1
    }


    inline var x: Float
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Float
        get() = elem1
        set(value) {
            elem1 = value
        }


    operator fun plus(other: Vec2f): Vec2f {
        return Vec2f(elem0 + other.elem0, elem1 + other.elem1)
    }

    operator fun plusAssign(other: Vec2f) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
    }

    operator fun plus(scalar: Float): Vec2f {
        return Vec2f(elem0 + scalar, elem1 + scalar)
    }

    operator fun plusAssign(scalar: Float) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
    }


    operator fun minus(other: Vec2f): Vec2f {
        return Vec2f(elem0 - other.elem0, elem1 - other.elem1)
    }

    operator fun minusAssign(other: Vec2f) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
    }

    operator fun minus(scalar: Float): Vec2f {
        return Vec2f(elem0 - scalar, elem1 - scalar)
    }

    operator fun minusAssign(scalar: Float) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
    }


    operator fun times(other: Vec2f): Vec2f {
        return Vec2f(elem0 * other.elem0, elem1 * other.elem1)
    }

    operator fun timesAssign(other: Vec2f) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
    }

    operator fun times(scalar: Float): Vec2f {
        return Vec2f(elem0 * scalar, elem1 * scalar)
    }

    operator fun timesAssign(scalar: Float) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
    }


    operator fun div(other: Vec2f): Vec2f {
        return Vec2f(elem0 / other.elem0, elem1 / other.elem1)
    }

    operator fun divAssign(other: Vec2f) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
    }

    operator fun div(scalar: Float): Vec2f {
        return Vec2f(elem0 / scalar, elem1 / scalar)
    }

    operator fun divAssign(scalar: Float) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
    }


    operator fun get(i: Int): Float {
        return when (i) {
            0 -> elem0
            1 -> elem1
            else -> error("Attempted to retrieve invalid element from 2 dimension vector")
        }
    }

    operator fun set(i: Int, t: Float) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            else -> error("Attempted to set invalid element from 2 dimension vector")
        }
    }

    operator fun get(axis: Axis): Float {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Float {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Float) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Float) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec2f): Float = elem0 * other.elem0 + elem1 * other.elem1

    fun magnitude2(): Float = elem0 * elem0 + elem1 * elem1

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1).toFloat())


    fun normalize() {
        val mag = magnitude()
        elem0 = elem0 / mag
        elem1 = elem1 / mag
    }

    fun normalizeSafe() {
        val mag2 = magnitude2()
        if (mag2 == 0.0f) {
            return
        }
        val mag = kotlin.math.sqrt(mag2)
        elem0 = elem0 / mag
        elem1 = elem1 / mag
    }


    override fun toString(): String {
        return "Vec2f($elem0,$elem1)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2f

        return elem0 == other.elem0 && elem1 == other.elem1
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        return result
    }
}


open class Vec3f(var elem0: Float = 0.toFloat(), var elem1: Float = 0.toFloat(), var elem2: Float = 0.toFloat()) {


    operator fun invoke(arg0: Float, arg1: Float, arg2: Float) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
    }


    inline var x: Float
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Float
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Float
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var r: Float
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Float
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Float
        get() = elem2
        set(value) {
            elem2 = value
        }


    operator fun plus(other: Vec3f): Vec3f {
        return Vec3f(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2)
    }

    operator fun plusAssign(other: Vec3f) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
        elem2 = (elem2 + other.elem2)
    }

    operator fun plus(scalar: Float): Vec3f {
        return Vec3f(elem0 + scalar, elem1 + scalar, elem2 + scalar)
    }

    operator fun plusAssign(scalar: Float) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
        elem2 = (elem2 + scalar)
    }


    operator fun minus(other: Vec3f): Vec3f {
        return Vec3f(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2)
    }

    operator fun minusAssign(other: Vec3f) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
        elem2 = (elem2 - other.elem2)
    }

    operator fun minus(scalar: Float): Vec3f {
        return Vec3f(elem0 - scalar, elem1 - scalar, elem2 - scalar)
    }

    operator fun minusAssign(scalar: Float) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
        elem2 = (elem2 - scalar)
    }


    operator fun times(other: Vec3f): Vec3f {
        return Vec3f(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2)
    }

    operator fun timesAssign(other: Vec3f) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
        elem2 = (elem2 * other.elem2)
    }

    operator fun times(scalar: Float): Vec3f {
        return Vec3f(elem0 * scalar, elem1 * scalar, elem2 * scalar)
    }

    operator fun timesAssign(scalar: Float) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
        elem2 = (elem2 * scalar)
    }


    operator fun div(other: Vec3f): Vec3f {
        return Vec3f(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2)
    }

    operator fun divAssign(other: Vec3f) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
        elem2 = (elem2 / other.elem2)
    }

    operator fun div(scalar: Float): Vec3f {
        return Vec3f(elem0 / scalar, elem1 / scalar, elem2 / scalar)
    }

    operator fun divAssign(scalar: Float) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
        elem2 = (elem2 / scalar)
    }


    operator fun get(i: Int): Float {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            else -> error("Attempted to retrieve invalid element from 3 dimension vector")
        }
    }

    operator fun set(i: Int, t: Float) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            else -> error("Attempted to set invalid element from 3 dimension vector")
        }
    }

    operator fun get(axis: Axis): Float {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Float {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Float) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Float) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec3f): Float = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2

    fun magnitude2(): Float = elem0 * elem0 + elem1 * elem1 + elem2 * elem2

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2).toFloat())


    fun normalize() {
        val mag = magnitude()
        elem0 = elem0 / mag
        elem1 = elem1 / mag
        elem2 = elem2 / mag
    }

    fun normalizeSafe() {
        val mag2 = magnitude2()
        if (mag2 == 0.0f) {
            return
        }
        val mag = kotlin.math.sqrt(mag2)
        elem0 = elem0 / mag
        elem1 = elem1 / mag
        elem2 = elem2 / mag
    }


    fun cross(other: Vec3f): Vec3f = Vec3f(elem1 * other.elem2 - other.elem1 * elem2, elem2 * other.elem0 - other.elem2 * elem0, elem0 * other.elem1 - other.elem0 * elem1)


    override fun toString(): String {
        return "Vec3f($elem0,$elem1,$elem2)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec3f

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        return result
    }
}


class Vec4f(var elem0: Float = 0.toFloat(), var elem1: Float = 0.toFloat(), var elem2: Float = 0.toFloat(), var elem3: Float = 0.toFloat()) {


    operator fun invoke(arg0: Float, arg1: Float, arg2: Float, arg3: Float) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }


    inline var x: Float
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Float
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Float
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var w: Float
        get() = elem3
        set(value) {
            elem3 = value
        }


    inline var r: Float
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Float
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Float
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var a: Float
        get() = elem3
        set(value) {
            elem3 = value
        }


    operator fun plus(other: Vec4f): Vec4f {
        return Vec4f(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2, elem3 + other.elem3)
    }

    operator fun plusAssign(other: Vec4f) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
        elem2 = (elem2 + other.elem2)
        elem3 = (elem3 + other.elem3)
    }

    operator fun plus(scalar: Float): Vec4f {
        return Vec4f(elem0 + scalar, elem1 + scalar, elem2 + scalar, elem3 + scalar)
    }

    operator fun plusAssign(scalar: Float) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
        elem2 = (elem2 + scalar)
        elem3 = (elem3 + scalar)
    }


    operator fun minus(other: Vec4f): Vec4f {
        return Vec4f(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2, elem3 - other.elem3)
    }

    operator fun minusAssign(other: Vec4f) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
        elem2 = (elem2 - other.elem2)
        elem3 = (elem3 - other.elem3)
    }

    operator fun minus(scalar: Float): Vec4f {
        return Vec4f(elem0 - scalar, elem1 - scalar, elem2 - scalar, elem3 - scalar)
    }

    operator fun minusAssign(scalar: Float) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
        elem2 = (elem2 - scalar)
        elem3 = (elem3 - scalar)
    }


    operator fun times(other: Vec4f): Vec4f {
        return Vec4f(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2, elem3 * other.elem3)
    }

    operator fun timesAssign(other: Vec4f) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
        elem2 = (elem2 * other.elem2)
        elem3 = (elem3 * other.elem3)
    }

    operator fun times(scalar: Float): Vec4f {
        return Vec4f(elem0 * scalar, elem1 * scalar, elem2 * scalar, elem3 * scalar)
    }

    operator fun timesAssign(scalar: Float) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
        elem2 = (elem2 * scalar)
        elem3 = (elem3 * scalar)
    }


    operator fun div(other: Vec4f): Vec4f {
        return Vec4f(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2, elem3 / other.elem3)
    }

    operator fun divAssign(other: Vec4f) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
        elem2 = (elem2 / other.elem2)
        elem3 = (elem3 / other.elem3)
    }

    operator fun div(scalar: Float): Vec4f {
        return Vec4f(elem0 / scalar, elem1 / scalar, elem2 / scalar, elem3 / scalar)
    }

    operator fun divAssign(scalar: Float) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
        elem2 = (elem2 / scalar)
        elem3 = (elem3 / scalar)
    }


    operator fun get(i: Int): Float {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            3 -> elem3
            else -> error("Attempted to retrieve invalid element from 4 dimension vector")
        }
    }

    operator fun set(i: Int, t: Float) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            3 -> elem3 = t
            else -> error("Attempted to set invalid element from 4 dimension vector")
        }
    }

    operator fun get(axis: Axis): Float {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Float {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Float) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Float) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec4f): Float = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2 + elem3 * other.elem3

    fun magnitude2(): Float = elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3).toFloat())


    fun normalize() {
        val mag = magnitude()
        elem0 = elem0 / mag
        elem1 = elem1 / mag
        elem2 = elem2 / mag
        elem3 = elem3 / mag
    }

    fun normalizeSafe() {
        val mag2 = magnitude2()
        if (mag2 == 0.0f) {
            return
        }
        val mag = kotlin.math.sqrt(mag2)
        elem0 = elem0 / mag
        elem1 = elem1 / mag
        elem2 = elem2 / mag
        elem3 = elem3 / mag
    }


    override fun toString(): String {
        return "Vec4f($elem0,$elem1,$elem2,$elem3)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec4f

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2 && elem3 == other.elem3
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        result = result * 31 + elem3.hashCode()
        return result
    }
}


open class Vec2i(var elem0: Int = 0.toInt(), var elem1: Int = 0.toInt()) {


    operator fun invoke(arg0: Int, arg1: Int) {
        elem0 = arg0
        elem1 = arg1
    }


    inline var x: Int
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Int
        get() = elem1
        set(value) {
            elem1 = value
        }


    operator fun plus(other: Vec2i): Vec2i {
        return Vec2i(elem0 + other.elem0, elem1 + other.elem1)
    }

    operator fun plusAssign(other: Vec2i) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
    }

    operator fun plus(scalar: Int): Vec2i {
        return Vec2i(elem0 + scalar, elem1 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
    }


    operator fun minus(other: Vec2i): Vec2i {
        return Vec2i(elem0 - other.elem0, elem1 - other.elem1)
    }

    operator fun minusAssign(other: Vec2i) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
    }

    operator fun minus(scalar: Int): Vec2i {
        return Vec2i(elem0 - scalar, elem1 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
    }


    operator fun times(other: Vec2i): Vec2i {
        return Vec2i(elem0 * other.elem0, elem1 * other.elem1)
    }

    operator fun timesAssign(other: Vec2i) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
    }

    operator fun times(scalar: Int): Vec2i {
        return Vec2i(elem0 * scalar, elem1 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
    }


    operator fun div(other: Vec2i): Vec2i {
        return Vec2i(elem0 / other.elem0, elem1 / other.elem1)
    }

    operator fun divAssign(other: Vec2i) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
    }

    operator fun div(scalar: Int): Vec2i {
        return Vec2i(elem0 / scalar, elem1 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
    }


    operator fun get(i: Int): Int {
        return when (i) {
            0 -> elem0
            1 -> elem1
            else -> error("Attempted to retrieve invalid element from 2 dimension vector")
        }
    }

    operator fun set(i: Int, t: Int) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            else -> error("Attempted to set invalid element from 2 dimension vector")
        }
    }

    operator fun get(axis: Axis): Int {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Int {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Int) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Int) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec2i): Int = elem0 * other.elem0 + elem1 * other.elem1

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1).toFloat())


    fun toFloat(): Vec2f {
        return Vec2f(elem0.toFloat(), elem1.toFloat())
    }

    override fun toString(): String {
        return "Vec2i($elem0,$elem1)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2i

        return elem0 == other.elem0 && elem1 == other.elem1
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        return result
    }
}


open class Vec3i(var elem0: Int = 0.toInt(), var elem1: Int = 0.toInt(), var elem2: Int = 0.toInt()) {


    operator fun invoke(arg0: Int, arg1: Int, arg2: Int) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
    }


    inline var x: Int
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Int
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Int
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var r: Int
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Int
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Int
        get() = elem2
        set(value) {
            elem2 = value
        }


    operator fun plus(other: Vec3i): Vec3i {
        return Vec3i(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2)
    }

    operator fun plusAssign(other: Vec3i) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
        elem2 = (elem2 + other.elem2)
    }

    operator fun plus(scalar: Int): Vec3i {
        return Vec3i(elem0 + scalar, elem1 + scalar, elem2 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
        elem2 = (elem2 + scalar)
    }


    operator fun minus(other: Vec3i): Vec3i {
        return Vec3i(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2)
    }

    operator fun minusAssign(other: Vec3i) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
        elem2 = (elem2 - other.elem2)
    }

    operator fun minus(scalar: Int): Vec3i {
        return Vec3i(elem0 - scalar, elem1 - scalar, elem2 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
        elem2 = (elem2 - scalar)
    }


    operator fun times(other: Vec3i): Vec3i {
        return Vec3i(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2)
    }

    operator fun timesAssign(other: Vec3i) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
        elem2 = (elem2 * other.elem2)
    }

    operator fun times(scalar: Int): Vec3i {
        return Vec3i(elem0 * scalar, elem1 * scalar, elem2 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
        elem2 = (elem2 * scalar)
    }


    operator fun div(other: Vec3i): Vec3i {
        return Vec3i(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2)
    }

    operator fun divAssign(other: Vec3i) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
        elem2 = (elem2 / other.elem2)
    }

    operator fun div(scalar: Int): Vec3i {
        return Vec3i(elem0 / scalar, elem1 / scalar, elem2 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
        elem2 = (elem2 / scalar)
    }


    operator fun get(i: Int): Int {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            else -> error("Attempted to retrieve invalid element from 3 dimension vector")
        }
    }

    operator fun set(i: Int, t: Int) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            else -> error("Attempted to set invalid element from 3 dimension vector")
        }
    }

    operator fun get(axis: Axis): Int {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Int {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Int) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Int) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec3i): Int = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1 + elem2 * elem2

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2).toFloat())


    fun cross(other: Vec3i): Vec3i = Vec3i(elem1 * other.elem2 - other.elem1 * elem2, elem2 * other.elem0 - other.elem2 * elem0, elem0 * other.elem1 - other.elem0 * elem1)

    fun toFloat(): Vec3f {
        return Vec3f(elem0.toFloat(), elem1.toFloat(), elem2.toFloat())
    }

    override fun toString(): String {
        return "Vec3i($elem0,$elem1,$elem2)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec3i

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        return result
    }
}


class Vec4i(var elem0: Int = 0.toInt(), var elem1: Int = 0.toInt(), var elem2: Int = 0.toInt(), var elem3: Int = 0.toInt()) {


    operator fun invoke(arg0: Int, arg1: Int, arg2: Int, arg3: Int) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }


    inline var x: Int
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Int
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Int
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var w: Int
        get() = elem3
        set(value) {
            elem3 = value
        }


    inline var r: Int
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Int
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Int
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var a: Int
        get() = elem3
        set(value) {
            elem3 = value
        }


    operator fun plus(other: Vec4i): Vec4i {
        return Vec4i(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2, elem3 + other.elem3)
    }

    operator fun plusAssign(other: Vec4i) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
        elem2 = (elem2 + other.elem2)
        elem3 = (elem3 + other.elem3)
    }

    operator fun plus(scalar: Int): Vec4i {
        return Vec4i(elem0 + scalar, elem1 + scalar, elem2 + scalar, elem3 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
        elem2 = (elem2 + scalar)
        elem3 = (elem3 + scalar)
    }


    operator fun minus(other: Vec4i): Vec4i {
        return Vec4i(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2, elem3 - other.elem3)
    }

    operator fun minusAssign(other: Vec4i) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
        elem2 = (elem2 - other.elem2)
        elem3 = (elem3 - other.elem3)
    }

    operator fun minus(scalar: Int): Vec4i {
        return Vec4i(elem0 - scalar, elem1 - scalar, elem2 - scalar, elem3 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
        elem2 = (elem2 - scalar)
        elem3 = (elem3 - scalar)
    }


    operator fun times(other: Vec4i): Vec4i {
        return Vec4i(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2, elem3 * other.elem3)
    }

    operator fun timesAssign(other: Vec4i) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
        elem2 = (elem2 * other.elem2)
        elem3 = (elem3 * other.elem3)
    }

    operator fun times(scalar: Int): Vec4i {
        return Vec4i(elem0 * scalar, elem1 * scalar, elem2 * scalar, elem3 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
        elem2 = (elem2 * scalar)
        elem3 = (elem3 * scalar)
    }


    operator fun div(other: Vec4i): Vec4i {
        return Vec4i(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2, elem3 / other.elem3)
    }

    operator fun divAssign(other: Vec4i) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
        elem2 = (elem2 / other.elem2)
        elem3 = (elem3 / other.elem3)
    }

    operator fun div(scalar: Int): Vec4i {
        return Vec4i(elem0 / scalar, elem1 / scalar, elem2 / scalar, elem3 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
        elem2 = (elem2 / scalar)
        elem3 = (elem3 / scalar)
    }


    operator fun get(i: Int): Int {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            3 -> elem3
            else -> error("Attempted to retrieve invalid element from 4 dimension vector")
        }
    }

    operator fun set(i: Int, t: Int) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            3 -> elem3 = t
            else -> error("Attempted to set invalid element from 4 dimension vector")
        }
    }

    operator fun get(axis: Axis): Int {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Int {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Int) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Int) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec4i): Int = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2 + elem3 * other.elem3

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3).toFloat())


    fun toFloat(): Vec4f {
        return Vec4f(elem0.toFloat(), elem1.toFloat(), elem2.toFloat(), elem3.toFloat())
    }

    override fun toString(): String {
        return "Vec4i($elem0,$elem1,$elem2,$elem3)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec4i

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2 && elem3 == other.elem3
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        result = result * 31 + elem3.hashCode()
        return result
    }
}


class Vec2s(var elem0: Short = 0.toShort(), var elem1: Short = 0.toShort()) {
    constructor(arg0: Int, arg1: Int) : this(arg0.toShort(), arg1.toShort()) {}


    operator fun invoke(arg0: Short, arg1: Short) {
        elem0 = arg0
        elem1 = arg1
    }


    inline var x: Short
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Short
        get() = elem1
        set(value) {
            elem1 = value
        }


    operator fun plus(other: Vec2s): Vec2s {
        return Vec2s(elem0 + other.elem0, elem1 + other.elem1)
    }

    operator fun plusAssign(other: Vec2s) {
        elem0 = (elem0 + other.elem0).toShort()
        elem1 = (elem1 + other.elem1).toShort()
    }

    operator fun plus(scalar: Int): Vec2s {
        return Vec2s(elem0 + scalar, elem1 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar).toShort()
        elem1 = (elem1 + scalar).toShort()
    }


    operator fun minus(other: Vec2s): Vec2s {
        return Vec2s(elem0 - other.elem0, elem1 - other.elem1)
    }

    operator fun minusAssign(other: Vec2s) {
        elem0 = (elem0 - other.elem0).toShort()
        elem1 = (elem1 - other.elem1).toShort()
    }

    operator fun minus(scalar: Int): Vec2s {
        return Vec2s(elem0 - scalar, elem1 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar).toShort()
        elem1 = (elem1 - scalar).toShort()
    }


    operator fun times(other: Vec2s): Vec2s {
        return Vec2s(elem0 * other.elem0, elem1 * other.elem1)
    }

    operator fun timesAssign(other: Vec2s) {
        elem0 = (elem0 * other.elem0).toShort()
        elem1 = (elem1 * other.elem1).toShort()
    }

    operator fun times(scalar: Int): Vec2s {
        return Vec2s(elem0 * scalar, elem1 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar).toShort()
        elem1 = (elem1 * scalar).toShort()
    }


    operator fun div(other: Vec2s): Vec2s {
        return Vec2s(elem0 / other.elem0, elem1 / other.elem1)
    }

    operator fun divAssign(other: Vec2s) {
        elem0 = (elem0 / other.elem0).toShort()
        elem1 = (elem1 / other.elem1).toShort()
    }

    operator fun div(scalar: Int): Vec2s {
        return Vec2s(elem0 / scalar, elem1 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar).toShort()
        elem1 = (elem1 / scalar).toShort()
    }


    operator fun get(i: Int): Short {
        return when (i) {
            0 -> elem0
            1 -> elem1
            else -> error("Attempted to retrieve invalid element from 2 dimension vector")
        }
    }

    operator fun set(i: Int, t: Short) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            else -> error("Attempted to set invalid element from 2 dimension vector")
        }
    }

    operator fun get(axis: Axis): Short {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Short {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Short) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Short) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec2s): Int = elem0 * other.elem0 + elem1 * other.elem1

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1).toFloat())


    fun toFloat(): Vec2f {
        return Vec2f(elem0.toFloat(), elem1.toFloat())
    }

    override fun toString(): String {
        return "Vec2s($elem0,$elem1)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2s

        return elem0 == other.elem0 && elem1 == other.elem1
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        return result
    }
}


class Vec3s(var elem0: Short = 0.toShort(), var elem1: Short = 0.toShort(), var elem2: Short = 0.toShort()) {
    constructor(arg0: Int, arg1: Int, arg2: Int) : this(arg0.toShort(), arg1.toShort(), arg2.toShort()) {}


    operator fun invoke(arg0: Short, arg1: Short, arg2: Short) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
    }


    inline var x: Short
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Short
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Short
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var r: Short
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Short
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Short
        get() = elem2
        set(value) {
            elem2 = value
        }


    operator fun plus(other: Vec3s): Vec3s {
        return Vec3s(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2)
    }

    operator fun plusAssign(other: Vec3s) {
        elem0 = (elem0 + other.elem0).toShort()
        elem1 = (elem1 + other.elem1).toShort()
        elem2 = (elem2 + other.elem2).toShort()
    }

    operator fun plus(scalar: Int): Vec3s {
        return Vec3s(elem0 + scalar, elem1 + scalar, elem2 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar).toShort()
        elem1 = (elem1 + scalar).toShort()
        elem2 = (elem2 + scalar).toShort()
    }


    operator fun minus(other: Vec3s): Vec3s {
        return Vec3s(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2)
    }

    operator fun minusAssign(other: Vec3s) {
        elem0 = (elem0 - other.elem0).toShort()
        elem1 = (elem1 - other.elem1).toShort()
        elem2 = (elem2 - other.elem2).toShort()
    }

    operator fun minus(scalar: Int): Vec3s {
        return Vec3s(elem0 - scalar, elem1 - scalar, elem2 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar).toShort()
        elem1 = (elem1 - scalar).toShort()
        elem2 = (elem2 - scalar).toShort()
    }


    operator fun times(other: Vec3s): Vec3s {
        return Vec3s(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2)
    }

    operator fun timesAssign(other: Vec3s) {
        elem0 = (elem0 * other.elem0).toShort()
        elem1 = (elem1 * other.elem1).toShort()
        elem2 = (elem2 * other.elem2).toShort()
    }

    operator fun times(scalar: Int): Vec3s {
        return Vec3s(elem0 * scalar, elem1 * scalar, elem2 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar).toShort()
        elem1 = (elem1 * scalar).toShort()
        elem2 = (elem2 * scalar).toShort()
    }


    operator fun div(other: Vec3s): Vec3s {
        return Vec3s(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2)
    }

    operator fun divAssign(other: Vec3s) {
        elem0 = (elem0 / other.elem0).toShort()
        elem1 = (elem1 / other.elem1).toShort()
        elem2 = (elem2 / other.elem2).toShort()
    }

    operator fun div(scalar: Int): Vec3s {
        return Vec3s(elem0 / scalar, elem1 / scalar, elem2 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar).toShort()
        elem1 = (elem1 / scalar).toShort()
        elem2 = (elem2 / scalar).toShort()
    }


    operator fun get(i: Int): Short {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            else -> error("Attempted to retrieve invalid element from 3 dimension vector")
        }
    }

    operator fun set(i: Int, t: Short) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            else -> error("Attempted to set invalid element from 3 dimension vector")
        }
    }

    operator fun get(axis: Axis): Short {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Short {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Short) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Short) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec3s): Int = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1 + elem2 * elem2

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2).toFloat())


    fun cross(other: Vec3s): Vec3s = Vec3s(elem1 * other.elem2 - other.elem1 * elem2, elem2 * other.elem0 - other.elem2 * elem0, elem0 * other.elem1 - other.elem0 * elem1)

    fun toFloat(): Vec3f {
        return Vec3f(elem0.toFloat(), elem1.toFloat(), elem2.toFloat())
    }

    override fun toString(): String {
        return "Vec3s($elem0,$elem1,$elem2)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec3s

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        return result
    }
}


class Vec4s(var elem0: Short = 0.toShort(), var elem1: Short = 0.toShort(), var elem2: Short = 0.toShort(), var elem3: Short = 0.toShort()) {
    constructor(arg0: Int, arg1: Int, arg2: Int, arg3: Int) : this(arg0.toShort(), arg1.toShort(), arg2.toShort(), arg3.toShort()) {}


    operator fun invoke(arg0: Short, arg1: Short, arg2: Short, arg3: Short) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }


    inline var x: Short
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Short
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Short
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var w: Short
        get() = elem3
        set(value) {
            elem3 = value
        }


    inline var r: Short
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Short
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Short
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var a: Short
        get() = elem3
        set(value) {
            elem3 = value
        }


    operator fun plus(other: Vec4s): Vec4s {
        return Vec4s(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2, elem3 + other.elem3)
    }

    operator fun plusAssign(other: Vec4s) {
        elem0 = (elem0 + other.elem0).toShort()
        elem1 = (elem1 + other.elem1).toShort()
        elem2 = (elem2 + other.elem2).toShort()
        elem3 = (elem3 + other.elem3).toShort()
    }

    operator fun plus(scalar: Int): Vec4s {
        return Vec4s(elem0 + scalar, elem1 + scalar, elem2 + scalar, elem3 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar).toShort()
        elem1 = (elem1 + scalar).toShort()
        elem2 = (elem2 + scalar).toShort()
        elem3 = (elem3 + scalar).toShort()
    }


    operator fun minus(other: Vec4s): Vec4s {
        return Vec4s(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2, elem3 - other.elem3)
    }

    operator fun minusAssign(other: Vec4s) {
        elem0 = (elem0 - other.elem0).toShort()
        elem1 = (elem1 - other.elem1).toShort()
        elem2 = (elem2 - other.elem2).toShort()
        elem3 = (elem3 - other.elem3).toShort()
    }

    operator fun minus(scalar: Int): Vec4s {
        return Vec4s(elem0 - scalar, elem1 - scalar, elem2 - scalar, elem3 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar).toShort()
        elem1 = (elem1 - scalar).toShort()
        elem2 = (elem2 - scalar).toShort()
        elem3 = (elem3 - scalar).toShort()
    }


    operator fun times(other: Vec4s): Vec4s {
        return Vec4s(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2, elem3 * other.elem3)
    }

    operator fun timesAssign(other: Vec4s) {
        elem0 = (elem0 * other.elem0).toShort()
        elem1 = (elem1 * other.elem1).toShort()
        elem2 = (elem2 * other.elem2).toShort()
        elem3 = (elem3 * other.elem3).toShort()
    }

    operator fun times(scalar: Int): Vec4s {
        return Vec4s(elem0 * scalar, elem1 * scalar, elem2 * scalar, elem3 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar).toShort()
        elem1 = (elem1 * scalar).toShort()
        elem2 = (elem2 * scalar).toShort()
        elem3 = (elem3 * scalar).toShort()
    }


    operator fun div(other: Vec4s): Vec4s {
        return Vec4s(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2, elem3 / other.elem3)
    }

    operator fun divAssign(other: Vec4s) {
        elem0 = (elem0 / other.elem0).toShort()
        elem1 = (elem1 / other.elem1).toShort()
        elem2 = (elem2 / other.elem2).toShort()
        elem3 = (elem3 / other.elem3).toShort()
    }

    operator fun div(scalar: Int): Vec4s {
        return Vec4s(elem0 / scalar, elem1 / scalar, elem2 / scalar, elem3 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar).toShort()
        elem1 = (elem1 / scalar).toShort()
        elem2 = (elem2 / scalar).toShort()
        elem3 = (elem3 / scalar).toShort()
    }


    operator fun get(i: Int): Short {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            3 -> elem3
            else -> error("Attempted to retrieve invalid element from 4 dimension vector")
        }
    }

    operator fun set(i: Int, t: Short) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            3 -> elem3 = t
            else -> error("Attempted to set invalid element from 4 dimension vector")
        }
    }

    operator fun get(axis: Axis): Short {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Short {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Short) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Short) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec4s): Int = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2 + elem3 * other.elem3

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3).toFloat())


    fun toFloat(): Vec4f {
        return Vec4f(elem0.toFloat(), elem1.toFloat(), elem2.toFloat(), elem3.toFloat())
    }

    override fun toString(): String {
        return "Vec4s($elem0,$elem1,$elem2,$elem3)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec4s

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2 && elem3 == other.elem3
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        result = result * 31 + elem3.hashCode()
        return result
    }
}


class Vec2b(var elem0: Byte = 0.toByte(), var elem1: Byte = 0.toByte()) {
    constructor(arg0: Int, arg1: Int) : this(arg0.toByte(), arg1.toByte()) {}


    operator fun invoke(arg0: Byte, arg1: Byte) {
        elem0 = arg0
        elem1 = arg1
    }


    inline var x: Byte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Byte
        get() = elem1
        set(value) {
            elem1 = value
        }


    operator fun plus(other: Vec2b): Vec2b {
        return Vec2b(elem0 + other.elem0, elem1 + other.elem1)
    }

    operator fun plusAssign(other: Vec2b) {
        elem0 = (elem0 + other.elem0).toByte()
        elem1 = (elem1 + other.elem1).toByte()
    }

    operator fun plus(scalar: Int): Vec2b {
        return Vec2b(elem0 + scalar, elem1 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar).toByte()
        elem1 = (elem1 + scalar).toByte()
    }


    operator fun minus(other: Vec2b): Vec2b {
        return Vec2b(elem0 - other.elem0, elem1 - other.elem1)
    }

    operator fun minusAssign(other: Vec2b) {
        elem0 = (elem0 - other.elem0).toByte()
        elem1 = (elem1 - other.elem1).toByte()
    }

    operator fun minus(scalar: Int): Vec2b {
        return Vec2b(elem0 - scalar, elem1 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar).toByte()
        elem1 = (elem1 - scalar).toByte()
    }


    operator fun times(other: Vec2b): Vec2b {
        return Vec2b(elem0 * other.elem0, elem1 * other.elem1)
    }

    operator fun timesAssign(other: Vec2b) {
        elem0 = (elem0 * other.elem0).toByte()
        elem1 = (elem1 * other.elem1).toByte()
    }

    operator fun times(scalar: Int): Vec2b {
        return Vec2b(elem0 * scalar, elem1 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar).toByte()
        elem1 = (elem1 * scalar).toByte()
    }


    operator fun div(other: Vec2b): Vec2b {
        return Vec2b(elem0 / other.elem0, elem1 / other.elem1)
    }

    operator fun divAssign(other: Vec2b) {
        elem0 = (elem0 / other.elem0).toByte()
        elem1 = (elem1 / other.elem1).toByte()
    }

    operator fun div(scalar: Int): Vec2b {
        return Vec2b(elem0 / scalar, elem1 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar).toByte()
        elem1 = (elem1 / scalar).toByte()
    }


    operator fun get(i: Int): Byte {
        return when (i) {
            0 -> elem0
            1 -> elem1
            else -> error("Attempted to retrieve invalid element from 2 dimension vector")
        }
    }

    operator fun set(i: Int, t: Byte) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            else -> error("Attempted to set invalid element from 2 dimension vector")
        }
    }

    operator fun get(axis: Axis): Byte {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Byte {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Byte) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Byte) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec2b): Int = elem0 * other.elem0 + elem1 * other.elem1

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1).toFloat())


    fun toFloat(): Vec2f {
        return Vec2f(elem0.toFloat(), elem1.toFloat())
    }

    override fun toString(): String {
        return "Vec2b($elem0,$elem1)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2b

        return elem0 == other.elem0 && elem1 == other.elem1
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        return result
    }
}


class Vec3b(var elem0: Byte = 0.toByte(), var elem1: Byte = 0.toByte(), var elem2: Byte = 0.toByte()) {
    constructor(arg0: Int, arg1: Int, arg2: Int) : this(arg0.toByte(), arg1.toByte(), arg2.toByte()) {}


    operator fun invoke(arg0: Byte, arg1: Byte, arg2: Byte) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
    }


    inline var x: Byte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Byte
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Byte
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var r: Byte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Byte
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Byte
        get() = elem2
        set(value) {
            elem2 = value
        }


    operator fun plus(other: Vec3b): Vec3b {
        return Vec3b(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2)
    }

    operator fun plusAssign(other: Vec3b) {
        elem0 = (elem0 + other.elem0).toByte()
        elem1 = (elem1 + other.elem1).toByte()
        elem2 = (elem2 + other.elem2).toByte()
    }

    operator fun plus(scalar: Int): Vec3b {
        return Vec3b(elem0 + scalar, elem1 + scalar, elem2 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar).toByte()
        elem1 = (elem1 + scalar).toByte()
        elem2 = (elem2 + scalar).toByte()
    }


    operator fun minus(other: Vec3b): Vec3b {
        return Vec3b(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2)
    }

    operator fun minusAssign(other: Vec3b) {
        elem0 = (elem0 - other.elem0).toByte()
        elem1 = (elem1 - other.elem1).toByte()
        elem2 = (elem2 - other.elem2).toByte()
    }

    operator fun minus(scalar: Int): Vec3b {
        return Vec3b(elem0 - scalar, elem1 - scalar, elem2 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar).toByte()
        elem1 = (elem1 - scalar).toByte()
        elem2 = (elem2 - scalar).toByte()
    }


    operator fun times(other: Vec3b): Vec3b {
        return Vec3b(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2)
    }

    operator fun timesAssign(other: Vec3b) {
        elem0 = (elem0 * other.elem0).toByte()
        elem1 = (elem1 * other.elem1).toByte()
        elem2 = (elem2 * other.elem2).toByte()
    }

    operator fun times(scalar: Int): Vec3b {
        return Vec3b(elem0 * scalar, elem1 * scalar, elem2 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar).toByte()
        elem1 = (elem1 * scalar).toByte()
        elem2 = (elem2 * scalar).toByte()
    }


    operator fun div(other: Vec3b): Vec3b {
        return Vec3b(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2)
    }

    operator fun divAssign(other: Vec3b) {
        elem0 = (elem0 / other.elem0).toByte()
        elem1 = (elem1 / other.elem1).toByte()
        elem2 = (elem2 / other.elem2).toByte()
    }

    operator fun div(scalar: Int): Vec3b {
        return Vec3b(elem0 / scalar, elem1 / scalar, elem2 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar).toByte()
        elem1 = (elem1 / scalar).toByte()
        elem2 = (elem2 / scalar).toByte()
    }


    operator fun get(i: Int): Byte {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            else -> error("Attempted to retrieve invalid element from 3 dimension vector")
        }
    }

    operator fun set(i: Int, t: Byte) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            else -> error("Attempted to set invalid element from 3 dimension vector")
        }
    }

    operator fun get(axis: Axis): Byte {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Byte {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Byte) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Byte) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec3b): Int = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1 + elem2 * elem2

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2).toFloat())


    fun cross(other: Vec3b): Vec3b = Vec3b(elem1 * other.elem2 - other.elem1 * elem2, elem2 * other.elem0 - other.elem2 * elem0, elem0 * other.elem1 - other.elem0 * elem1)

    fun toFloat(): Vec3f {
        return Vec3f(elem0.toFloat(), elem1.toFloat(), elem2.toFloat())
    }

    override fun toString(): String {
        return "Vec3b($elem0,$elem1,$elem2)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec3b

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        return result
    }
}


class Vec4b(var elem0: Byte = 0.toByte(), var elem1: Byte = 0.toByte(), var elem2: Byte = 0.toByte(), var elem3: Byte = 0.toByte()) {
    constructor(arg0: Int, arg1: Int, arg2: Int, arg3: Int) : this(arg0.toByte(), arg1.toByte(), arg2.toByte(), arg3.toByte()) {}


    operator fun invoke(arg0: Byte, arg1: Byte, arg2: Byte, arg3: Byte) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }


    inline var x: Byte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Byte
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Byte
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var w: Byte
        get() = elem3
        set(value) {
            elem3 = value
        }


    inline var r: Byte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Byte
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Byte
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var a: Byte
        get() = elem3
        set(value) {
            elem3 = value
        }


    operator fun plus(other: Vec4b): Vec4b {
        return Vec4b(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2, elem3 + other.elem3)
    }

    operator fun plusAssign(other: Vec4b) {
        elem0 = (elem0 + other.elem0).toByte()
        elem1 = (elem1 + other.elem1).toByte()
        elem2 = (elem2 + other.elem2).toByte()
        elem3 = (elem3 + other.elem3).toByte()
    }

    operator fun plus(scalar: Int): Vec4b {
        return Vec4b(elem0 + scalar, elem1 + scalar, elem2 + scalar, elem3 + scalar)
    }

    operator fun plusAssign(scalar: Int) {
        elem0 = (elem0 + scalar).toByte()
        elem1 = (elem1 + scalar).toByte()
        elem2 = (elem2 + scalar).toByte()
        elem3 = (elem3 + scalar).toByte()
    }


    operator fun minus(other: Vec4b): Vec4b {
        return Vec4b(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2, elem3 - other.elem3)
    }

    operator fun minusAssign(other: Vec4b) {
        elem0 = (elem0 - other.elem0).toByte()
        elem1 = (elem1 - other.elem1).toByte()
        elem2 = (elem2 - other.elem2).toByte()
        elem3 = (elem3 - other.elem3).toByte()
    }

    operator fun minus(scalar: Int): Vec4b {
        return Vec4b(elem0 - scalar, elem1 - scalar, elem2 - scalar, elem3 - scalar)
    }

    operator fun minusAssign(scalar: Int) {
        elem0 = (elem0 - scalar).toByte()
        elem1 = (elem1 - scalar).toByte()
        elem2 = (elem2 - scalar).toByte()
        elem3 = (elem3 - scalar).toByte()
    }


    operator fun times(other: Vec4b): Vec4b {
        return Vec4b(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2, elem3 * other.elem3)
    }

    operator fun timesAssign(other: Vec4b) {
        elem0 = (elem0 * other.elem0).toByte()
        elem1 = (elem1 * other.elem1).toByte()
        elem2 = (elem2 * other.elem2).toByte()
        elem3 = (elem3 * other.elem3).toByte()
    }

    operator fun times(scalar: Int): Vec4b {
        return Vec4b(elem0 * scalar, elem1 * scalar, elem2 * scalar, elem3 * scalar)
    }

    operator fun timesAssign(scalar: Int) {
        elem0 = (elem0 * scalar).toByte()
        elem1 = (elem1 * scalar).toByte()
        elem2 = (elem2 * scalar).toByte()
        elem3 = (elem3 * scalar).toByte()
    }


    operator fun div(other: Vec4b): Vec4b {
        return Vec4b(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2, elem3 / other.elem3)
    }

    operator fun divAssign(other: Vec4b) {
        elem0 = (elem0 / other.elem0).toByte()
        elem1 = (elem1 / other.elem1).toByte()
        elem2 = (elem2 / other.elem2).toByte()
        elem3 = (elem3 / other.elem3).toByte()
    }

    operator fun div(scalar: Int): Vec4b {
        return Vec4b(elem0 / scalar, elem1 / scalar, elem2 / scalar, elem3 / scalar)
    }

    operator fun divAssign(scalar: Int) {
        elem0 = (elem0 / scalar).toByte()
        elem1 = (elem1 / scalar).toByte()
        elem2 = (elem2 / scalar).toByte()
        elem3 = (elem3 / scalar).toByte()
    }


    operator fun get(i: Int): Byte {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            3 -> elem3
            else -> error("Attempted to retrieve invalid element from 4 dimension vector")
        }
    }

    operator fun set(i: Int, t: Byte) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            3 -> elem3 = t
            else -> error("Attempted to set invalid element from 4 dimension vector")
        }
    }

    operator fun get(axis: Axis): Byte {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Byte {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Byte) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Byte) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec4b): Int = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2 + elem3 * other.elem3

    fun magnitude2(): Int = elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3).toFloat())


    fun toFloat(): Vec4f {
        return Vec4f(elem0.toFloat(), elem1.toFloat(), elem2.toFloat(), elem3.toFloat())
    }

    override fun toString(): String {
        return "Vec4b($elem0,$elem1,$elem2,$elem3)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec4b

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2 && elem3 == other.elem3
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        result = result * 31 + elem3.hashCode()
        return result
    }
}


class Vec2ub(var elem0: UByte = 0.toUByte(), var elem1: UByte = 0.toUByte()) {
    constructor(arg0: UInt, arg1: UInt) : this(arg0.toUByte(), arg1.toUByte()) {}


    operator fun invoke(arg0: UByte, arg1: UByte) {
        elem0 = arg0
        elem1 = arg1
    }


    inline var x: UByte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: UByte
        get() = elem1
        set(value) {
            elem1 = value
        }


    operator fun plus(other: Vec2ub): Vec2ub {
        return Vec2ub(elem0 + other.elem0, elem1 + other.elem1)
    }

    operator fun plusAssign(other: Vec2ub) {
        elem0 = (elem0 + other.elem0).toUByte()
        elem1 = (elem1 + other.elem1).toUByte()
    }

    operator fun plus(scalar: UInt): Vec2ub {
        return Vec2ub(elem0 + scalar, elem1 + scalar)
    }

    operator fun plusAssign(scalar: UInt) {
        elem0 = (elem0 + scalar).toUByte()
        elem1 = (elem1 + scalar).toUByte()
    }


    operator fun minus(other: Vec2ub): Vec2ub {
        return Vec2ub(elem0 - other.elem0, elem1 - other.elem1)
    }

    operator fun minusAssign(other: Vec2ub) {
        elem0 = (elem0 - other.elem0).toUByte()
        elem1 = (elem1 - other.elem1).toUByte()
    }

    operator fun minus(scalar: UInt): Vec2ub {
        return Vec2ub(elem0 - scalar, elem1 - scalar)
    }

    operator fun minusAssign(scalar: UInt) {
        elem0 = (elem0 - scalar).toUByte()
        elem1 = (elem1 - scalar).toUByte()
    }


    operator fun times(other: Vec2ub): Vec2ub {
        return Vec2ub(elem0 * other.elem0, elem1 * other.elem1)
    }

    operator fun timesAssign(other: Vec2ub) {
        elem0 = (elem0 * other.elem0).toUByte()
        elem1 = (elem1 * other.elem1).toUByte()
    }

    operator fun times(scalar: UInt): Vec2ub {
        return Vec2ub(elem0 * scalar, elem1 * scalar)
    }

    operator fun timesAssign(scalar: UInt) {
        elem0 = (elem0 * scalar).toUByte()
        elem1 = (elem1 * scalar).toUByte()
    }


    operator fun div(other: Vec2ub): Vec2ub {
        return Vec2ub(elem0 / other.elem0, elem1 / other.elem1)
    }

    operator fun divAssign(other: Vec2ub) {
        elem0 = (elem0 / other.elem0).toUByte()
        elem1 = (elem1 / other.elem1).toUByte()
    }

    operator fun div(scalar: UInt): Vec2ub {
        return Vec2ub(elem0 / scalar, elem1 / scalar)
    }

    operator fun divAssign(scalar: UInt) {
        elem0 = (elem0 / scalar).toUByte()
        elem1 = (elem1 / scalar).toUByte()
    }


    operator fun get(i: Int): UByte {
        return when (i) {
            0 -> elem0
            1 -> elem1
            else -> error("Attempted to retrieve invalid element from 2 dimension vector")
        }
    }

    operator fun set(i: Int, t: UByte) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            else -> error("Attempted to set invalid element from 2 dimension vector")
        }
    }

    operator fun get(axis: Axis): UByte {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): UByte {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: UByte) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: UByte) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec2ub): UInt = elem0 * other.elem0 + elem1 * other.elem1

    fun magnitude2(): UInt = elem0 * elem0 + elem1 * elem1

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1).toFloat())


    fun toFloat(): Vec2f {
        return Vec2f(elem0.toFloat(), elem1.toFloat())
    }

    override fun toString(): String {
        return "Vec2ub($elem0,$elem1)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2ub

        return elem0 == other.elem0 && elem1 == other.elem1
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        return result
    }
}


class Vec3ub(var elem0: UByte = 0.toUByte(), var elem1: UByte = 0.toUByte(), var elem2: UByte = 0.toUByte()) {
    constructor(arg0: UInt, arg1: UInt, arg2: UInt) : this(arg0.toUByte(), arg1.toUByte(), arg2.toUByte()) {}


    operator fun invoke(arg0: UByte, arg1: UByte, arg2: UByte) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
    }


    inline var x: UByte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: UByte
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: UByte
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var r: UByte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: UByte
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: UByte
        get() = elem2
        set(value) {
            elem2 = value
        }


    operator fun plus(other: Vec3ub): Vec3ub {
        return Vec3ub(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2)
    }

    operator fun plusAssign(other: Vec3ub) {
        elem0 = (elem0 + other.elem0).toUByte()
        elem1 = (elem1 + other.elem1).toUByte()
        elem2 = (elem2 + other.elem2).toUByte()
    }

    operator fun plus(scalar: UInt): Vec3ub {
        return Vec3ub(elem0 + scalar, elem1 + scalar, elem2 + scalar)
    }

    operator fun plusAssign(scalar: UInt) {
        elem0 = (elem0 + scalar).toUByte()
        elem1 = (elem1 + scalar).toUByte()
        elem2 = (elem2 + scalar).toUByte()
    }


    operator fun minus(other: Vec3ub): Vec3ub {
        return Vec3ub(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2)
    }

    operator fun minusAssign(other: Vec3ub) {
        elem0 = (elem0 - other.elem0).toUByte()
        elem1 = (elem1 - other.elem1).toUByte()
        elem2 = (elem2 - other.elem2).toUByte()
    }

    operator fun minus(scalar: UInt): Vec3ub {
        return Vec3ub(elem0 - scalar, elem1 - scalar, elem2 - scalar)
    }

    operator fun minusAssign(scalar: UInt) {
        elem0 = (elem0 - scalar).toUByte()
        elem1 = (elem1 - scalar).toUByte()
        elem2 = (elem2 - scalar).toUByte()
    }


    operator fun times(other: Vec3ub): Vec3ub {
        return Vec3ub(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2)
    }

    operator fun timesAssign(other: Vec3ub) {
        elem0 = (elem0 * other.elem0).toUByte()
        elem1 = (elem1 * other.elem1).toUByte()
        elem2 = (elem2 * other.elem2).toUByte()
    }

    operator fun times(scalar: UInt): Vec3ub {
        return Vec3ub(elem0 * scalar, elem1 * scalar, elem2 * scalar)
    }

    operator fun timesAssign(scalar: UInt) {
        elem0 = (elem0 * scalar).toUByte()
        elem1 = (elem1 * scalar).toUByte()
        elem2 = (elem2 * scalar).toUByte()
    }


    operator fun div(other: Vec3ub): Vec3ub {
        return Vec3ub(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2)
    }

    operator fun divAssign(other: Vec3ub) {
        elem0 = (elem0 / other.elem0).toUByte()
        elem1 = (elem1 / other.elem1).toUByte()
        elem2 = (elem2 / other.elem2).toUByte()
    }

    operator fun div(scalar: UInt): Vec3ub {
        return Vec3ub(elem0 / scalar, elem1 / scalar, elem2 / scalar)
    }

    operator fun divAssign(scalar: UInt) {
        elem0 = (elem0 / scalar).toUByte()
        elem1 = (elem1 / scalar).toUByte()
        elem2 = (elem2 / scalar).toUByte()
    }


    operator fun get(i: Int): UByte {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            else -> error("Attempted to retrieve invalid element from 3 dimension vector")
        }
    }

    operator fun set(i: Int, t: UByte) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            else -> error("Attempted to set invalid element from 3 dimension vector")
        }
    }

    operator fun get(axis: Axis): UByte {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): UByte {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: UByte) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: UByte) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec3ub): UInt = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2

    fun magnitude2(): UInt = elem0 * elem0 + elem1 * elem1 + elem2 * elem2

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2).toFloat())


    fun cross(other: Vec3ub): Vec3ub = Vec3ub(elem1 * other.elem2 - other.elem1 * elem2, elem2 * other.elem0 - other.elem2 * elem0, elem0 * other.elem1 - other.elem0 * elem1)

    fun toFloat(): Vec3f {
        return Vec3f(elem0.toFloat(), elem1.toFloat(), elem2.toFloat())
    }

    override fun toString(): String {
        return "Vec3ub($elem0,$elem1,$elem2)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec3ub

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        return result
    }
}





class Vec4ub(var elem0: UByte = 0.toUByte(), var elem1: UByte = 0.toUByte(), var elem2: UByte = 0.toUByte(), var elem3: UByte = 0.toUByte()) {
    constructor(arg0: UInt, arg1: UInt, arg2: UInt, arg3: UInt) : this(arg0.toUByte(), arg1.toUByte(), arg2.toUByte(), arg3.toUByte()) {}


    operator fun invoke(arg0: UByte, arg1: UByte, arg2: UByte, arg3: UByte) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }


    inline var x: UByte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: UByte
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: UByte
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var w: UByte
        get() = elem3
        set(value) {
            elem3 = value
        }


    inline var r: UByte
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: UByte
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: UByte
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var a: UByte
        get() = elem3
        set(value) {
            elem3 = value
        }


    operator fun plus(other: Vec4ub): Vec4ub {
        return Vec4ub(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2, elem3 + other.elem3)
    }

    operator fun plusAssign(other: Vec4ub) {
        elem0 = (elem0 + other.elem0).toUByte()
        elem1 = (elem1 + other.elem1).toUByte()
        elem2 = (elem2 + other.elem2).toUByte()
        elem3 = (elem3 + other.elem3).toUByte()
    }

    operator fun plus(scalar: UInt): Vec4ub {
        return Vec4ub(elem0 + scalar, elem1 + scalar, elem2 + scalar, elem3 + scalar)
    }

    operator fun plusAssign(scalar: UInt) {
        elem0 = (elem0 + scalar).toUByte()
        elem1 = (elem1 + scalar).toUByte()
        elem2 = (elem2 + scalar).toUByte()
        elem3 = (elem3 + scalar).toUByte()
    }


    operator fun minus(other: Vec4ub): Vec4ub {
        return Vec4ub(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2, elem3 - other.elem3)
    }

    operator fun minusAssign(other: Vec4ub) {
        elem0 = (elem0 - other.elem0).toUByte()
        elem1 = (elem1 - other.elem1).toUByte()
        elem2 = (elem2 - other.elem2).toUByte()
        elem3 = (elem3 - other.elem3).toUByte()
    }

    operator fun minus(scalar: UInt): Vec4ub {
        return Vec4ub(elem0 - scalar, elem1 - scalar, elem2 - scalar, elem3 - scalar)
    }

    operator fun minusAssign(scalar: UInt) {
        elem0 = (elem0 - scalar).toUByte()
        elem1 = (elem1 - scalar).toUByte()
        elem2 = (elem2 - scalar).toUByte()
        elem3 = (elem3 - scalar).toUByte()
    }


    operator fun times(other: Vec4ub): Vec4ub {
        return Vec4ub(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2, elem3 * other.elem3)
    }

    operator fun timesAssign(other: Vec4ub) {
        elem0 = (elem0 * other.elem0).toUByte()
        elem1 = (elem1 * other.elem1).toUByte()
        elem2 = (elem2 * other.elem2).toUByte()
        elem3 = (elem3 * other.elem3).toUByte()
    }

    operator fun times(scalar: UInt): Vec4ub {
        return Vec4ub(elem0 * scalar, elem1 * scalar, elem2 * scalar, elem3 * scalar)
    }

    operator fun timesAssign(scalar: UInt) {
        elem0 = (elem0 * scalar).toUByte()
        elem1 = (elem1 * scalar).toUByte()
        elem2 = (elem2 * scalar).toUByte()
        elem3 = (elem3 * scalar).toUByte()
    }


    operator fun div(other: Vec4ub): Vec4ub {
        return Vec4ub(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2, elem3 / other.elem3)
    }

    operator fun divAssign(other: Vec4ub) {
        elem0 = (elem0 / other.elem0).toUByte()
        elem1 = (elem1 / other.elem1).toUByte()
        elem2 = (elem2 / other.elem2).toUByte()
        elem3 = (elem3 / other.elem3).toUByte()
    }

    operator fun div(scalar: UInt): Vec4ub {
        return Vec4ub(elem0 / scalar, elem1 / scalar, elem2 / scalar, elem3 / scalar)
    }

    operator fun divAssign(scalar: UInt) {
        elem0 = (elem0 / scalar).toUByte()
        elem1 = (elem1 / scalar).toUByte()
        elem2 = (elem2 / scalar).toUByte()
        elem3 = (elem3 / scalar).toUByte()
    }


    operator fun get(i: Int): UByte {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            3 -> elem3
            else -> error("Attempted to retrieve invalid element from 4 dimension vector")
        }
    }

    operator fun set(i: Int, t: UByte) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            3 -> elem3 = t
            else -> error("Attempted to set invalid element from 4 dimension vector")
        }
    }

    operator fun get(axis: Axis): UByte {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): UByte {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: UByte) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: UByte) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec4ub): UInt = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2 + elem3 * other.elem3

    fun magnitude2(): UInt = elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3).toFloat())


    fun toFloat(): Vec4f {
        return Vec4f(elem0.toFloat(), elem1.toFloat(), elem2.toFloat(), elem3.toFloat())
    }

    override fun toString(): String {
        return "Vec4ub($elem0,$elem1,$elem2,$elem3)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec4ub

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2 && elem3 == other.elem3
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        result = result * 31 + elem3.hashCode()
        return result
    }
}


class Vec2d(var elem0: Double = 0.toDouble(), var elem1: Double = 0.toDouble()) {


    operator fun invoke(arg0: Double, arg1: Double) {
        elem0 = arg0
        elem1 = arg1
    }


    inline var x: Double
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Double
        get() = elem1
        set(value) {
            elem1 = value
        }


    operator fun plus(other: Vec2d): Vec2d {
        return Vec2d(elem0 + other.elem0, elem1 + other.elem1)
    }

    operator fun plusAssign(other: Vec2d) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
    }

    operator fun plus(scalar: Double): Vec2d {
        return Vec2d(elem0 + scalar, elem1 + scalar)
    }

    operator fun plusAssign(scalar: Double) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
    }


    operator fun minus(other: Vec2d): Vec2d {
        return Vec2d(elem0 - other.elem0, elem1 - other.elem1)
    }

    operator fun minusAssign(other: Vec2d) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
    }

    operator fun minus(scalar: Double): Vec2d {
        return Vec2d(elem0 - scalar, elem1 - scalar)
    }

    operator fun minusAssign(scalar: Double) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
    }


    operator fun times(other: Vec2d): Vec2d {
        return Vec2d(elem0 * other.elem0, elem1 * other.elem1)
    }

    operator fun timesAssign(other: Vec2d) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
    }

    operator fun times(scalar: Double): Vec2d {
        return Vec2d(elem0 * scalar, elem1 * scalar)
    }

    operator fun timesAssign(scalar: Double) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
    }


    operator fun div(other: Vec2d): Vec2d {
        return Vec2d(elem0 / other.elem0, elem1 / other.elem1)
    }

    operator fun divAssign(other: Vec2d) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
    }

    operator fun div(scalar: Double): Vec2d {
        return Vec2d(elem0 / scalar, elem1 / scalar)
    }

    operator fun divAssign(scalar: Double) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
    }


    operator fun get(i: Int): Double {
        return when (i) {
            0 -> elem0
            1 -> elem1
            else -> error("Attempted to retrieve invalid element from 2 dimension vector")
        }
    }

    operator fun set(i: Int, t: Double) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            else -> error("Attempted to set invalid element from 2 dimension vector")
        }
    }

    operator fun get(axis: Axis): Double {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Double {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Double) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Double) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec2d): Double = elem0 * other.elem0 + elem1 * other.elem1

    fun magnitude2(): Double = elem0 * elem0 + elem1 * elem1

    fun magnitude(): Double = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1).toDouble())


    fun normalize() {
        val mag = magnitude()
        elem0 = elem0 / mag
        elem1 = elem1 / mag
    }

    fun normalizeSafe() {
        val mag2 = magnitude2()
        if (mag2 == 0.0) {
            return
        }
        val mag = kotlin.math.sqrt(mag2)
        elem0 = elem0 / mag
        elem1 = elem1 / mag
    }


    override fun toString(): String {
        return "Vec2d($elem0,$elem1)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2d

        return elem0 == other.elem0 && elem1 == other.elem1
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        return result
    }
}


class Vec3d(var elem0: Double = 0.toDouble(), var elem1: Double = 0.toDouble(), var elem2: Double = 0.toDouble()) {


    operator fun invoke(arg0: Double, arg1: Double, arg2: Double) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
    }


    inline var x: Double
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Double
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Double
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var r: Double
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Double
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Double
        get() = elem2
        set(value) {
            elem2 = value
        }


    operator fun plus(other: Vec3d): Vec3d {
        return Vec3d(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2)
    }

    operator fun plusAssign(other: Vec3d) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
        elem2 = (elem2 + other.elem2)
    }

    operator fun plus(scalar: Double): Vec3d {
        return Vec3d(elem0 + scalar, elem1 + scalar, elem2 + scalar)
    }

    operator fun plusAssign(scalar: Double) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
        elem2 = (elem2 + scalar)
    }


    operator fun minus(other: Vec3d): Vec3d {
        return Vec3d(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2)
    }

    operator fun minusAssign(other: Vec3d) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
        elem2 = (elem2 - other.elem2)
    }

    operator fun minus(scalar: Double): Vec3d {
        return Vec3d(elem0 - scalar, elem1 - scalar, elem2 - scalar)
    }

    operator fun minusAssign(scalar: Double) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
        elem2 = (elem2 - scalar)
    }


    operator fun times(other: Vec3d): Vec3d {
        return Vec3d(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2)
    }

    operator fun timesAssign(other: Vec3d) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
        elem2 = (elem2 * other.elem2)
    }

    operator fun times(scalar: Double): Vec3d {
        return Vec3d(elem0 * scalar, elem1 * scalar, elem2 * scalar)
    }

    operator fun timesAssign(scalar: Double) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
        elem2 = (elem2 * scalar)
    }


    operator fun div(other: Vec3d): Vec3d {
        return Vec3d(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2)
    }

    operator fun divAssign(other: Vec3d) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
        elem2 = (elem2 / other.elem2)
    }

    operator fun div(scalar: Double): Vec3d {
        return Vec3d(elem0 / scalar, elem1 / scalar, elem2 / scalar)
    }

    operator fun divAssign(scalar: Double) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
        elem2 = (elem2 / scalar)
    }


    operator fun get(i: Int): Double {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            else -> error("Attempted to retrieve invalid element from 3 dimension vector")
        }
    }

    operator fun set(i: Int, t: Double) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            else -> error("Attempted to set invalid element from 3 dimension vector")
        }
    }

    operator fun get(axis: Axis): Double {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Double {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Double) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Double) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec3d): Double = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2

    fun magnitude2(): Double = elem0 * elem0 + elem1 * elem1 + elem2 * elem2

    fun magnitude(): Double = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2).toDouble())


    fun normalize() {
        val mag = magnitude()
        elem0 = elem0 / mag
        elem1 = elem1 / mag
        elem2 = elem2 / mag
    }

    fun normalizeSafe() {
        val mag2 = magnitude2()
        if (mag2 == 0.0) {
            return
        }
        val mag = kotlin.math.sqrt(mag2)
        elem0 = elem0 / mag
        elem1 = elem1 / mag
        elem2 = elem2 / mag
    }


    fun cross(other: Vec3d): Vec3d = Vec3d(elem1 * other.elem2 - other.elem1 * elem2, elem2 * other.elem0 - other.elem2 * elem0, elem0 * other.elem1 - other.elem0 * elem1)


    override fun toString(): String {
        return "Vec3d($elem0,$elem1,$elem2)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec3d

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        return result
    }
}


class Vec4d(var elem0: Double = 0.toDouble(), var elem1: Double = 0.toDouble(), var elem2: Double = 0.toDouble(), var elem3: Double = 0.toDouble()) {


    operator fun invoke(arg0: Double, arg1: Double, arg2: Double, arg3: Double) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }


    inline var x: Double
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: Double
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: Double
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var w: Double
        get() = elem3
        set(value) {
            elem3 = value
        }


    inline var r: Double
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: Double
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: Double
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var a: Double
        get() = elem3
        set(value) {
            elem3 = value
        }


    operator fun plus(other: Vec4d): Vec4d {
        return Vec4d(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2, elem3 + other.elem3)
    }

    operator fun plusAssign(other: Vec4d) {
        elem0 = (elem0 + other.elem0)
        elem1 = (elem1 + other.elem1)
        elem2 = (elem2 + other.elem2)
        elem3 = (elem3 + other.elem3)
    }

    operator fun plus(scalar: Double): Vec4d {
        return Vec4d(elem0 + scalar, elem1 + scalar, elem2 + scalar, elem3 + scalar)
    }

    operator fun plusAssign(scalar: Double) {
        elem0 = (elem0 + scalar)
        elem1 = (elem1 + scalar)
        elem2 = (elem2 + scalar)
        elem3 = (elem3 + scalar)
    }


    operator fun minus(other: Vec4d): Vec4d {
        return Vec4d(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2, elem3 - other.elem3)
    }

    operator fun minusAssign(other: Vec4d) {
        elem0 = (elem0 - other.elem0)
        elem1 = (elem1 - other.elem1)
        elem2 = (elem2 - other.elem2)
        elem3 = (elem3 - other.elem3)
    }

    operator fun minus(scalar: Double): Vec4d {
        return Vec4d(elem0 - scalar, elem1 - scalar, elem2 - scalar, elem3 - scalar)
    }

    operator fun minusAssign(scalar: Double) {
        elem0 = (elem0 - scalar)
        elem1 = (elem1 - scalar)
        elem2 = (elem2 - scalar)
        elem3 = (elem3 - scalar)
    }


    operator fun times(other: Vec4d): Vec4d {
        return Vec4d(elem0 * other.elem0, elem1 * other.elem1, elem2 * other.elem2, elem3 * other.elem3)
    }

    operator fun timesAssign(other: Vec4d) {
        elem0 = (elem0 * other.elem0)
        elem1 = (elem1 * other.elem1)
        elem2 = (elem2 * other.elem2)
        elem3 = (elem3 * other.elem3)
    }

    operator fun times(scalar: Double): Vec4d {
        return Vec4d(elem0 * scalar, elem1 * scalar, elem2 * scalar, elem3 * scalar)
    }

    operator fun timesAssign(scalar: Double) {
        elem0 = (elem0 * scalar)
        elem1 = (elem1 * scalar)
        elem2 = (elem2 * scalar)
        elem3 = (elem3 * scalar)
    }


    operator fun div(other: Vec4d): Vec4d {
        return Vec4d(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2, elem3 / other.elem3)
    }

    operator fun divAssign(other: Vec4d) {
        elem0 = (elem0 / other.elem0)
        elem1 = (elem1 / other.elem1)
        elem2 = (elem2 / other.elem2)
        elem3 = (elem3 / other.elem3)
    }

    operator fun div(scalar: Double): Vec4d {
        return Vec4d(elem0 / scalar, elem1 / scalar, elem2 / scalar, elem3 / scalar)
    }

    operator fun divAssign(scalar: Double) {
        elem0 = (elem0 / scalar)
        elem1 = (elem1 / scalar)
        elem2 = (elem2 / scalar)
        elem3 = (elem3 / scalar)
    }


    operator fun get(i: Int): Double {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            3 -> elem3
            else -> error("Attempted to retrieve invalid element from 4 dimension vector")
        }
    }

    operator fun set(i: Int, t: Double) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            3 -> elem3 = t
            else -> error("Attempted to set invalid element from 4 dimension vector")
        }
    }

    operator fun get(axis: Axis): Double {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): Double {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: Double) {
        return set(axis.ordinal, t)
    }

    operator fun get(axis: Axis2D, t: Double) {
        return set(axis.ordinal, t)
    }


    fun dot(other: Vec4d): Double = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2 + elem3 * other.elem3

    fun magnitude2(): Double = elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3

    fun magnitude(): Double = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3).toDouble())


    fun normalize() {
        val mag = magnitude()
        elem0 = elem0 / mag
        elem1 = elem1 / mag
        elem2 = elem2 / mag
        elem3 = elem3 / mag
    }

    fun normalizeSafe() {
        val mag2 = magnitude2()
        if (mag2 == 0.0) {
            return
        }
        val mag = kotlin.math.sqrt(mag2)
        elem0 = elem0 / mag
        elem1 = elem1 / mag
        elem2 = elem2 / mag
        elem3 = elem3 / mag
    }


    override fun toString(): String {
        return "Vec4d($elem0,$elem1,$elem2,$elem3)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec4d

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2 && elem3 == other.elem3
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        result = result * 31 + elem3.hashCode()
        return result
    }
}


data class Vec2<T>(var elem0: T, var elem1: T) {

    inline var x: T
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: T
        get() = elem1
        set(value) {
            elem1 = value
        }


    operator fun get(i: Int): T {
        return when (i) {
            0 -> elem0
            1 -> elem1
            else -> error("Attempted to retrieve invalid element from 2 dimension vector")
        }
    }

    operator fun set(i: Int, t: T) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            else -> error("Attempted to set invalid element from 2 dimension vector")
        }
    }

    operator fun get(axis: Axis): T {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): T {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: T) {
        return set(axis.ordinal, t)
    }

    operator fun set(axis: Axis2D, t: T) {
        return set(axis.ordinal, t)
    }


    operator fun invoke(arg0: T, arg1: T) {
        elem0 = arg0
        elem1 = arg1
    }

    fun set(arg0: T, arg1: T) {
        elem0 = arg0
        elem1 = arg1
    }


    override fun toString(): String {
        return "Vec2($elem0,$elem1)"
    }
}

data class Vec3<T>(var elem0: T, var elem1: T, var elem2: T) {

    inline var x: T
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: T
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: T
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var r: T
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: T
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: T
        get() = elem2
        set(value) {
            elem2 = value
        }


    operator fun get(i: Int): T {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            else -> error("Attempted to retrieve invalid element from 3 dimension vector")
        }
    }

    operator fun set(i: Int, t: T) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            else -> error("Attempted to set invalid element from 3 dimension vector")
        }
    }

    operator fun get(axis: Axis): T {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): T {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: T) {
        return set(axis.ordinal, t)
    }

    operator fun set(axis: Axis2D, t: T) {
        return set(axis.ordinal, t)
    }


    operator fun invoke(arg0: T, arg1: T, arg2: T) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
    }

    fun set(arg0: T, arg1: T, arg2: T) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
    }


    override fun toString(): String {
        return "Vec3($elem0,$elem1,$elem2)"
    }
}

data class Vec4<T>(var elem0: T, var elem1: T, var elem2: T, var elem3: T) {

    inline var x: T
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var y: T
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var z: T
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var w: T
        get() = elem3
        set(value) {
            elem3 = value
        }


    inline var r: T
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var g: T
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var b: T
        get() = elem2
        set(value) {
            elem2 = value
        }


    inline var a: T
        get() = elem3
        set(value) {
            elem3 = value
        }


    operator fun get(i: Int): T {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            3 -> elem3
            else -> error("Attempted to retrieve invalid element from 4 dimension vector")
        }
    }

    operator fun set(i: Int, t: T) {
        when (i) {
            0 -> elem0 = t
            1 -> elem1 = t
            2 -> elem2 = t
            3 -> elem3 = t
            else -> error("Attempted to set invalid element from 4 dimension vector")
        }
    }

    operator fun get(axis: Axis): T {
        return get(axis.ordinal)
    }

    operator fun get(axis: Axis2D): T {
        return get(axis.ordinal)
    }

    operator fun set(axis: Axis, t: T) {
        return set(axis.ordinal, t)
    }

    operator fun set(axis: Axis2D, t: T) {
        return set(axis.ordinal, t)
    }


    operator fun invoke(arg0: T, arg1: T, arg2: T, arg3: T) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }

    fun set(arg0: T, arg1: T, arg2: T, arg3: T) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }


    override fun toString(): String {
        return "Vec4($elem0,$elem1,$elem2,$elem3)"
    }
}
