package hfcom.display

import arx.core.*
import kotlin.math.roundToInt

class MapCoord2D(x : Int, y : Int) : Vec2i(x,y) {
    companion object {
        init {
            registerCustomTypeRender<MapCoord2D>()
        }
        fun project(x : Int, y : Int) : Vec2f {
            return Vec2f(x * 0.5f - (y * 0.5f), x * 0.25f + y * 0.25f)
        }

        fun project(x : Int, y : Int, v : Vec2f) {
            v.x = x * 0.5f - (y * 0.5f)
            v.y = x * 0.25f + y * 0.25f
        }

        fun unproject(v : Vec2f) : MapCoord2D {
            return MapCoord2D(kotlin.math.round(v.x + v.y * 2.0f).toInt(), kotlin.math.round(v.y * 2.0f - v.x).toInt())
        }
    }

    fun project() : Vec2f {
        return MapCoord2D.project(x,y)
    }

    fun distanceTo(to: MapCoord2D): Float {
        val d2 = (to.toFloat() - this.toFloat()).magnitude2()
        return if (d2 != 0.0f) {
            kotlin.math.sqrt(d2)
        } else {
            return 0.0f
        }
    }

    override fun toString(): String {
        return "MapCoord2D($x, $y)"
    }
}

class MapCoord(x : Int, y : Int, z : Int) : Vec3i(x,y,z) {
    constructor (c2d: MapCoord2D, z: Int): this(c2d.x, c2d.y, z)

    val xy : MapCoord2D get() { return MapCoord2D(x,y) }

    companion object {
        init {
            registerCustomTypeRender<MapCoord>()
        }

        fun project(x : Int, y : Int, z : Int) : Vec3f {
            return Vec3f(x * 0.5f - (y * 0.5f), x * 0.25f + y * 0.25f + z * 0.25f, 0.0f)
        }

        fun project(x : Int, y : Int, z : Int, v : Vec3f) {
            v.x = x * 0.5f - (y * 0.5f)
            v.y = x * 0.25f + y * 0.25f + z * 0.25f
            v.z = 0.0f
        }

        fun unproject(v : Vec2f) : MapCoord {
            return MapCoord(kotlin.math.round(v.x + v.y * 2.0f).toInt(), kotlin.math.round(v.y * 2.0f - v.x).toInt(), 0)
        }
    }

    fun project() : Vec3f {
        return MapCoord.project(x,y,z)
    }

    fun project(v: Vec3f) {
        return MapCoord.project(x,y,z, v)
    }

    fun distanceTo(to: MapCoord): Float {
        val d2 = (to.toFloat() - this.toFloat()).magnitude2()
        return if (d2 != 0.0f) {
            kotlin.math.sqrt(d2)
        } else {
            return 0.0f
        }
    }

    fun toMapCoordf() : MapCoordf {
        return MapCoordf(x.toFloat(), y.toFloat(), z.toFloat())
    }

    override fun toString(): String {
        return "MapCoord($x, $y, $z)"
    }
}

class MapCoordf(x : Float, y : Float, z : Float) : Vec3f(x,y,z) {
    constructor (v: Vec3f) : this(v.x, v.y, v.z)

    companion object {
        init {
            registerCustomTypeRender<MapCoordf>()
        }
        fun project(x : Float, y : Float, z : Float) : Vec3f {
            return Vec3f(x * 0.5f - (y * 0.5f), x * 0.25f + y * 0.25f + z * 0.25f, 0.0f)
        }
    }

    fun project() : Vec3f {
        return MapCoordf.project(x,y,z)
    }

    fun toMapCoord(round : Boolean = true) : MapCoord {
        return if (round) {
            MapCoord(x.roundToInt(), y.roundToInt(), z.roundToInt())
        } else {
            MapCoord(x.toInt(), y.toInt(), z.toInt())
        }
    }

    fun toMapCoord2D(round : Boolean = true) : MapCoord2D {
        return if (round) {
            MapCoord2D(x.roundToInt(), y.roundToInt())
        } else {
            MapCoord2D(x.toInt(), y.toInt())
        }
    }

    override fun toString(): String {
        return "MapCoordf($x, $y, $z)"
    }
}