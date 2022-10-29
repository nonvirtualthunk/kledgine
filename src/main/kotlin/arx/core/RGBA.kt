package arx.core

import com.typesafe.config.ConfigValue
import java.awt.Color
import java.lang.Integer.min


enum class RGBAChannel {
    Red,
    Green,
    Blue,
    Alpha;


    companion object {
        val colorChannels = arrayOf(Red, Green, Blue)
    }
}

class RGBA(var elem0: UByte = 0.toUByte(), var elem1: UByte = 0.toUByte(), var elem2: UByte = 0.toUByte(), var elem3: UByte = 0.toUByte()) {
    constructor(arg0: UInt, arg1: UInt, arg2: UInt, arg3: UInt) : this(arg0.toUByte(), arg1.toUByte(), arg2.toUByte(), arg3.toUByte()) {}

    companion object : FromConfigCreator<RGBA> {
        override fun createFromConfig(cv: ConfigValue?): RGBA? {
            if (cv.isStr()) {
                val str = cv.asStr() ?: ""
                if (str.startsWith("#")) {
                    try {
                        val color = Color.decode(str)
                        return RGBA(color.red, color.green, color.blue, 255)
                    } catch (e : NumberFormatException) {
                        Noto.warn("Invalid hex color : $cv")
                    }
                }
            } else if (cv.isList()) {
                val listElems = cv.asList()
                if (listElems.size in 3..4) {
                    val r = listElems[0].asFloat()
                    val g = listElems[1].asFloat()
                    val b = listElems[2].asFloat()
                    val a = listElems.getOrNull(3).asFloat()

                    if (r != null && g != null && b != null) {
                        return if (r <= 1.0f && g <= 1.0f && b <= 1.0f) {
                            RGBAf(r, g, b, a ?: 1.0f)
                        } else {
                            RGBA(r.toInt(), g.toInt(), b.toInt(), a?.toInt() ?: 255)
                        }
                    } else {
                        Noto.warn("Invalid components for RGBA color : $cv")
                    }
                }
            }

            return null
        }
    }

    /*
    if v.isStr:
    let str = v.asStr
    if str.startsWith("#") or str.startsWith("rgb"):
      let parsed = chroma.parseHtmlColor(str)
      color = rgba(parsed.r, parsed.g, parsed.b, 1.0f)
    else:
      warn &"Color could not be parsed: {str}"
  else:
    let elems = v.asArr
    if elems.len != 4:
      warn &"RGBA cannot be read, less than 4 elements: {v}"
    else:
      if elems[0].asFloat <= 1.0f and elems[1].asFloat <= 1.0f and elems[2].asFloat <= 1.0f and elems[3].asFloat <= 1.0f:
        color = rgba(elems[0].asFloat, elems[1].asFloat, elems[2].asFloat, elems[3].asFloat)
      else:
        color = rgba(elems[0].asInt.uint8, elems[1].asInt.uint8, elems[2].asInt.uint8, elems[3].asInt.uint8)
     */


    operator fun invoke(arg0: UByte, arg1: UByte, arg2: UByte, arg3: UByte) {
        elem0 = arg0
        elem1 = arg1
        elem2 = arg2
        elem3 = arg3
    }

    operator fun invoke(other : RGBA) {
        elem0 = other.elem0
        elem1 = other.elem1
        elem2 = other.elem2
        elem3 = other.elem3
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


    operator fun plus(other: RGBA): RGBA {
        return RGBA(elem0 + other.elem0, elem1 + other.elem1, elem2 + other.elem2, elem3 + other.elem3)
    }

    operator fun plusAssign(other: RGBA) {
        elem0 = (elem0 + other.elem0).toUByte()
        elem1 = (elem1 + other.elem1).toUByte()
        elem2 = (elem2 + other.elem2).toUByte()
        elem3 = (elem3 + other.elem3).toUByte()
    }

    operator fun plus(scalar: UInt): RGBA {
        return RGBA(elem0 + scalar, elem1 + scalar, elem2 + scalar, elem3 + scalar)
    }

    operator fun plusAssign(scalar: UInt) {
        elem0 = (elem0 + scalar).toUByte()
        elem1 = (elem1 + scalar).toUByte()
        elem2 = (elem2 + scalar).toUByte()
        elem3 = (elem3 + scalar).toUByte()
    }


    operator fun minus(other: RGBA): RGBA {
        return RGBA(elem0 - other.elem0, elem1 - other.elem1, elem2 - other.elem2, elem3 - other.elem3)
    }

    operator fun minusAssign(other: RGBA) {
        elem0 = (elem0 - other.elem0).toUByte()
        elem1 = (elem1 - other.elem1).toUByte()
        elem2 = (elem2 - other.elem2).toUByte()
        elem3 = (elem3 - other.elem3).toUByte()
    }

    operator fun minus(scalar: UInt): RGBA {
        return RGBA(elem0 - scalar, elem1 - scalar, elem2 - scalar, elem3 - scalar)
    }

    operator fun minusAssign(scalar: UInt) {
        elem0 = (elem0 - scalar).toUByte()
        elem1 = (elem1 - scalar).toUByte()
        elem2 = (elem2 - scalar).toUByte()
        elem3 = (elem3 - scalar).toUByte()
    }


    operator fun times(other: RGBA): RGBA {
        return RGBA((elem0 * other.elem0) / 255u, (elem1 * other.elem1) / 255u, (elem2 * other.elem2) / 255u, (elem3 * other.elem3) / 255u)
    }

    operator fun timesAssign(other: RGBA) {
        elem0 = ((elem0 * other.elem0) / 255u).toUByte()
        elem1 = ((elem1 * other.elem1) / 255u).toUByte()
        elem2 = ((elem2 * other.elem2) / 255u).toUByte()
        elem3 = ((elem3 * other.elem3) / 255u).toUByte()
    }

    operator fun times(scalar: UInt): RGBA {
        return RGBA(elem0 * scalar, elem1 * scalar, elem2 * scalar, elem3 * scalar)
    }

//    operator fun timesAssign(scalar: UInt) {
//        elem0 = (elem0 * scalar).toUByte()
//        elem1 = (elem1 * scalar).toUByte()
//        elem2 = (elem2 * scalar).toUByte()
//        elem3 = (elem3 * scalar).toUByte()
//    }


//    operator fun div(other: RGBA): RGBA {
//        return RGBA(elem0 / other.elem0, elem1 / other.elem1, elem2 / other.elem2, elem3 / other.elem3)
//    }

//    operator fun divAssign(other: RGBA) {
//        elem0 = (elem0 / other.elem0).toUByte()
//        elem1 = (elem1 / other.elem1).toUByte()
//        elem2 = (elem2 / other.elem2).toUByte()
//        elem3 = (elem3 / other.elem3).toUByte()
//    }

//    operator fun div(scalar: UInt): RGBA {
//        return RGBA(elem0 / scalar, elem1 / scalar, elem2 / scalar, elem3 / scalar)
//    }
//
//    operator fun divAssign(scalar: UInt) {
//        elem0 = (elem0 / scalar).toUByte()
//        elem1 = (elem1 / scalar).toUByte()
//        elem2 = (elem2 / scalar).toUByte()
//        elem3 = (elem3 / scalar).toUByte()
//    }


    operator fun get(i: Int): UByte {
        return when (i) {
            0 -> elem0
            1 -> elem1
            2 -> elem2
            3 -> elem3
            else -> error("Attempted to retrieve invalid element from 4 dimension vector")
        }
    }

    operator fun get(c : RGBAChannel): UByte {
        return when (c) {
            RGBAChannel.Red -> elem0
            RGBAChannel.Green -> elem1
            RGBAChannel.Blue -> elem2
            RGBAChannel.Alpha -> elem3
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

    operator fun set(i: RGBAChannel, t: UByte) {
        when (i) {
            RGBAChannel.Red -> elem0 = t
            RGBAChannel.Green -> elem1 = t
            RGBAChannel.Blue -> elem2 = t
            RGBAChannel.Alpha -> elem3 = t
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


    fun dot(other: RGBA): UInt = elem0 * other.elem0 + elem1 * other.elem1 + elem2 * other.elem2 + elem3 * other.elem3

    fun magnitude2(): UInt = elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3

    fun magnitude(): Float = kotlin.math.sqrt((elem0 * elem0 + elem1 * elem1 + elem2 * elem2 + elem3 * elem3).toFloat())

    fun minWith(other : RGBA) {
        r = minu(other.r, r)
        g = minu(other.g, g)
        b = minu(other.b, b)
        a = minu(other.a, a)
    }

    fun maxWith(other : RGBA) {
        r = maxu(other.r, r)
        g = maxu(other.g, g)
        b = maxu(other.b, b)
        a = maxu(other.a, a)
    }

    fun toFloat(): Vec4f {
        return Vec4f(elem0.toFloat() / 255.0f, elem1.toFloat() / 255.0f, elem2.toFloat() / 255.0f, elem3.toFloat() / 255.0f)
    }

    override fun toString(): String {
        return "RGBA($elem0,$elem1,$elem2,$elem3)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RGBA

        return elem0 == other.elem0 && elem1 == other.elem1 && elem2 == other.elem2 && elem3 == other.elem3
    }

    override fun hashCode(): Int {
        var result = elem0.hashCode()
        result = result * 31 + elem1.hashCode()
        result = result * 31 + elem2.hashCode()
        result = result * 31 + elem3.hashCode()
        return result
    }

    fun toHSL(): HSL {
        return toHSL(this)
    }
}

fun RGBA(r: Int, g: Int, b: Int, a: Int): RGBA {
    return RGBA(r.clamp(0,255).toUByte(), g.clamp(0,255).toUByte(), b.clamp(0,255).toUByte(), a.clamp(0,255).toUByte())
}

fun RGBAf(r: Float, g: Float, b: Float, a: Float): RGBA {
    return RGBA((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), (a * 255).toInt())
}
fun RGBAf(v : Vec4f): RGBA {
    return RGBAf(v.r, v.g, v.b, v.a)
}

fun average(rgbas : Iterable<RGBA>) : RGBA {
    val v = Vec4i()
    var count = 0
    for (c in rgbas) {
        v.r += c.r.toInt()
        v.g += c.g.toInt()
        v.b += c.b.toInt()
        v.a += c.a.toInt()
        count += 1
    }
    return RGBA(
        v.r / count,
        v.g / count,
        v.b / count,
        v.a / count
    )
}

fun HueToRGB(p: Float, q: Float, hIn: Float): Float {
    var h = hIn
    if (h < 0) h += 1f
    if (h > 1) h -= 1f
    if (6 * h < 1) {
        return p + (q - p) * 6 * h
    }
    if (2 * h < 1) {
        return q
    }
    return if (3 * h < 2) {
        p + (q - p) * 6 * (2.0f / 3.0f - h)
    } else p
}
fun fromHSL(v : Vec3f) : RGBA {
    val h = v.x
    val s = v.y
    val l = v.z
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

fun toHSL(c: RGBA): HSL {
    val cf = c.toFloat()
    val minc = kotlin.math.min(kotlin.math.min(cf.r, cf.g), cf.b)
    val maxc = Math.max(Math.max(cf.r, cf.g), cf.b)

    val h = if (maxc == minc) {
        0.0f
    } else if (maxc == cf.r) {
        ((60.0f * (cf.g - cf.b) / (maxc - minc)) + 360.0f) % 360.0f
    } else if (maxc == cf.g) {
        (60.0f * (cf.b - cf.r) / (maxc - minc)) + 120.0f
    } else {
        (60.0f * (cf.r - cf.g) / (maxc - minc)) + 240.0f
    }

    val l = (maxc + minc) / 2.0f

    val s = if (maxc == minc) {
        0.0f
    } else if (l <= 0.5f) {
        (maxc - minc) / (maxc + minc)
    } else {
        (maxc - minc) / (2.0f - maxc - minc);
    }

    return HSL(h / 360.0f, s, l, c.a.toFloat() / 255.0f)
}


fun max(a : RGBA, b : RGBA) : RGBA {
    return RGBA(maxu(a.r,b.r), maxu(a.g,b.g), maxu(a.b,b.b), maxu(a.a,b.a))
}

fun max(a : RGBA, b : RGBA, v : RGBA) {
    v(maxu(a.r,b.r), maxu(a.g,b.g), maxu(a.b,b.b), maxu(a.a,b.a))
}


val White = RGBA(255,255,255,255)
val Black = RGBA(0,0,0,255)
val Clear = RGBA(255,255,255,0)