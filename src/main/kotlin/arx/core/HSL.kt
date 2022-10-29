package arx.core


enum class HSLChannel {
    Hue,
    Saturation,
    Lightness,
    Alpha;


    companion object {
        val colorChannels = arrayOf(Hue, Saturation, Lightness)
    }
}


class HSL(var elem0: Float = 0.toFloat(), var elem1: Float = 0.toFloat(), var elem2: Float = 0.toFloat(), var elem3: Float = 1.toFloat()) {


    operator fun invoke(arg0: Float, arg1: Float, arg2: Float, arg3: Float) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }


    inline var h: Float
        get() = elem0
        set(value) {
            elem0 = value
        }


    inline var s: Float
        get() = elem1
        set(value) {
            elem1 = value
        }


    inline var l: Float
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

    operator fun get(i: HSLChannel): Float {
        return when (i) {
            HSLChannel.Hue -> elem0
            HSLChannel.Saturation -> elem1
            HSLChannel.Lightness -> elem2
            HSLChannel.Alpha -> elem3
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

    operator fun set(i: HSLChannel, t: Float) {
        return when (i) {
            HSLChannel.Hue -> elem0 = t
            HSLChannel.Saturation -> elem1 = t
            HSLChannel.Lightness -> elem2 = t
            HSLChannel.Alpha -> elem3 = t
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

    fun minWith(other: HSL) {
        h = other.h.min(h)
        s = other.s.min(s)
        l = other.l.min(l)
        a = other.a.min(a)
    }

    fun maxWith(other: HSL) {
        h = other.h.max(h)
        s = other.s.max(s)
        l = other.l.max(l)
        a = other.a.max(a)
    }

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

    fun toRGBA() : RGBA {
        val q = if (l < 0.5f){ l * (1f + s) } else { l + s - s * l }

        val p: Float = 2f * l - q

        var r = Math.max(0f, HueToRGB(p, q, h + 1.0f / 3.0f))
        var g = Math.max(0f, HueToRGB(p, q, h))
        var b = Math.max(0f, HueToRGB(p, q, h - 1.0f / 3.0f))

        r = Math.min(r, 1.0f)
        g = Math.min(g, 1.0f)
        b = Math.min(b, 1.0f)

        return RGBAf(r, g, b, 1.0f)
    }
}


