package arx.core

data class Recti(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int
) {
    constructor (pos: Vec2i, dim: Vec2i) : this(pos.x, pos.y, dim.x, dim.y)

    var dimensions
        get() : Vec2i { return Vec2i(width, height) }
        set(v: Vec2i) { width = v.x; height = v.y }

    var position
        get() : Vec2i { return Vec2i(x, y) }
        set(v: Vec2i) { x = v.x; y = v.y }

    fun max() : Vec2i {
        return Vec2i(x + width, y + height)
    }
    fun min() : Vec2i {
        return position
    }

    fun intersect(b: Recti) : Recti {
        val a = this
        val minX = kotlin.math.max(a.position.x, b.position.x)
        val minY = kotlin.math.max(a.position.y, b.position.y)
        val maxX = kotlin.math.min(a.position.x + a.dimensions.x, b.position.x + b.dimensions.x)
        val maxY = kotlin.math.min(a.position.y + a.dimensions.y, b.position.y + b.dimensions.y)
        return if (maxX >= minX && maxY >= minY) {
            Recti(minX, minY, maxX - minX, maxY - minY)
        } else {
            Recti(0,0,0,0)
        }
    }

    /*
    proc intersect*[T](a: Rect[T], b: Rect[T]): Rect[T] =
   let minX = max(a.position.x, b.position.x)
   let minY = max(a.position.y, b.position.y)
   let maxX = min(a.position.x + a.dimensions.x, b.position.x + b.dimensions.x)
   let maxY = min(a.position.y + a.dimensions.y, b.position.y + b.dimensions.y)
   if maxX >= minX and maxY >= minY:
     result.position.x = minX
     result.position.y = minY
     result.dimensions.x = maxX - minX
     result.dimensions.y = maxY - minY
     */
}

data class Rectf(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float
) {
    constructor (pos: Vec2f, dim: Vec2f) : this(pos.x, pos.y, dim.x, dim.y)

    var dimensions
        get() : Vec2f { return Vec2f(width, height) }
        set(v: Vec2f) { width = v.x; height = v.y }

    var position
        get() : Vec2f { return Vec2f(x, y) }
        set(v: Vec2f) { x = v.x; y = v.y }

    fun max() : Vec2f {
        return Vec2f(x + width, y + height)
    }
    fun min() : Vec2f {
        return position
    }
}