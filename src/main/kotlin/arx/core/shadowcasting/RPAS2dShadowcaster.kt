package arx.core.shadowcasting

import arx.core.ShadowGrid
import arx.core.Shadowcaster
import arx.core.Vec3i
import kotlin.math.PI
import kotlin.math.atan2

internal data class Obstruction(val near: Float, val far : Float, val opacity : Float)
internal data class CircleItem(var x : Int, var y : Int, var near : Float, var center : Float, var far : Float)
internal data class CircleItemVariant(var x : Int, var y : Int, var a : Float, var b : Float, var c : Float, var center : Float)

data class Octant(val qx : Int, val qy : Int, val vert : Boolean) {
    internal fun tiles(maxR : Int) : Iterator<CircleItem> {
        return iterator {
            val v = CircleItem(0, 0, 0f, 0f, 0f)
            yield(v)

            val maxr2 = maxR * maxR
            for (r in 0 .. maxR) {
                val rf = r.toFloat()
                for (x in 0 .. r) {
                    v.x = if (vert) {
                        x * qx
                    } else {
                        r * qx
                    }

                    v.y = if (vert) {
                        r * qy
                    } else {
                        x * qy
                    }

                    if (v.x*v.x + v.y*v.y > maxr2) {
                        continue
                    }

                    val angleAlloc = 1f / (rf + 1.0f)
                    v.near = x * angleAlloc
                    v.center = v.near + 0.5f * angleAlloc
                    v.far = v.near + angleAlloc

                    yield(v)
                }
            }
        }
    }
}


data class OctantVariant(val qx : Int, val qy : Int, val vert : Boolean) {
    companion object {
        fun initRArrays(r : Int) : Array<FloatArray> {
            return Array(r) { i -> FloatArray(i + 1) }
        }

        val aAngles = initRArrays(31)
        val bAngles = initRArrays(31)
        val cAngles = initRArrays(31)
        val centerAngles = initRArrays(31)

        init {
            var i = 0
            for (r in 1 .. 30) {
                val rf = r.toFloat()
                for (x in 0 .. r) {
                    bAngles[r][x] = atan2(x - 0.5f, rf - 0.5f) / (PI / 4.0f).toFloat() // close near corner
                    cAngles[r][x] = atan2(x + 0.5f, rf - 0.5f) / (PI / 4.0f).toFloat() // close far corner
                    aAngles[r][x] = atan2(x - 0.5f, rf + 0.5f) / (PI / 4.0f).toFloat() // dist near corner
                    centerAngles[r][x] = atan2(x.toFloat(), rf) / (PI / 4.0f).toFloat() // dist near corner
                    i++
                }
            }
            println(i)
        }
    }

    internal fun tiles(maxR : Int) : Iterator<CircleItemVariant> {
        return iterator {
            val v = CircleItemVariant(0, 0, 0f, 0f, 0f, 0f)
            yield(v)

            val maxr2 = maxR * maxR
            var i = 0
            for (r in 1 .. maxR) {
                for (x in 0 .. r) {
                    v.x = if (vert) {
                        x * qx
                    } else {
                        r * qx
                    }

                    v.y = if (vert) {
                        r * qy
                    } else {
                        x * qy
                    }

                    if (v.x*v.x + v.y*v.y > maxr2) {
                        continue
                    }

                    v.b = bAngles[r][x]
                    v.a = aAngles[r][x]
                    v.c = cAngles[r][x]
                    v.center = centerAngles[r][x]
                    i++


                    yield(v)
                }
            }
        }
    }
}


fun octants() : Iterator<Octant> {
    return iterator {
        for (x in listOf(1, -1)) {
            for (y in listOf(1, -1)) {
                for (vert in arrayOf(true, false)) {
                    yield(Octant(x, y, vert))
                }
            }
        }
    }
}

fun octantVariants() : Iterator<OctantVariant> {
    return iterator {
        for (x in listOf(1, -1)) {
            for (y in listOf(1, -1)) {
                for (vert in arrayOf(true, false)) {
                    yield(OctantVariant(x,y,vert))
                }
            }
        }
    }
}

class RPAS2dShadowcaster(
    val opacityFn: (Int, Int, Int) -> Float, // the opacity of a given world coordinate
    val filterFn: (Int, Int, Int) -> Boolean, // filter for deciding whether the given coordinate should be considered, input in world coordinates
) : Shadowcaster {

    val nonvisOcclude = false
    val useVariant2dShadowcast = true

    fun angleContainedIn(angle : Float, start: Float, end : Float) : Boolean {
        return angle > start && angle < end
    }

    fun visibleWhen(center: Boolean, near: Boolean, far: Boolean): Boolean {
        return center && (near || far)
    }

    override fun shadowcast(out: ShadowGrid, origin: Vec3i, radius: Int) {
        if (useVariant2dShadowcast) {
            shadowcastVariant(out, origin, radius)
        } else {
            shadowcastOriginal(out, origin, radius)
        }
    }

    override fun shadowcast(origin: Vec3i, radius: Int, out: (Int, Int, Int, Float) -> Unit) {
        TODO("Not yet implemented")
    }

    fun shadowcastOriginal(out : ShadowGrid, origin : Vec3i, radius : Int) {
        out.init(origin, radius)

        for (octant in octants()) {
            val obstructions = mutableListOf<Obstruction>()
            for (tile in octant.tiles(radius)) {
                var visible = true
                var nearVis = true
                var centerVis = true
                var farVis = true

                for (obs in obstructions) {
                    nearVis = nearVis && !angleContainedIn(angle = tile.near, start = obs.near, end = obs.far)
                    centerVis = centerVis && !angleContainedIn(angle = tile.center, start = obs.near, end = obs.far)
                    farVis = farVis && !angleContainedIn(angle = tile.far, start = obs.near, end = obs.far)

                    visible = visibleWhen(center = centerVis, near = nearVis, far = farVis)
                    if (! visible) {
                        break
                    }
                }

                val opacity = opacityFn(tile.x + origin.x, tile.y + origin.y, origin.z)
                if ((nonvisOcclude && !visible) || opacity > 0.0f) {
                    obstructions.add(Obstruction(near = tile.near, far = tile.far, opacity = opacity))
                }

                out.setAtShadowCoord(tile.x, tile.y, 0, if (visible) { 1.0f } else { 0.0f })
            }
        }
    }

    fun shadowcastVariant(out : ShadowGrid, origin : Vec3i, radius : Int) {
        out.init(origin, radius)

        for (octant in octantVariants()) {
            val obstructions = mutableListOf<Obstruction>()
            for (tile in octant.tiles(radius)) {
                var visible = true
                var distFar = true
                var distCenter = true
                var closeNear = true
                var closeCenter = true
                var closeFar = true
                var center = true

                val opacity = opacityFn(tile.x + origin.x, tile.y + origin.y, origin.z)

                val centerWiggle = (tile.b - tile.a) * 0.35f

                for (obs in obstructions) {
                    distFar = distFar && !angleContainedIn(angle = tile.a, start = obs.near, end = obs.far)
                    distCenter = distCenter && !angleContainedIn(angle = (tile.a + tile.b) * 0.5f, start = obs.near, end = obs.far)
                    closeNear = closeNear && !angleContainedIn(angle = tile.b, start = obs.near, end = obs.far)
                    closeCenter = closeCenter && !angleContainedIn(angle = (tile.b + tile.c) * 0.5f, start = obs.near, end = obs.far)
                    closeFar = closeFar && !angleContainedIn(angle = tile.c, start = obs.near, end = obs.far)
                    center = center && (!angleContainedIn(angle = tile.center + centerWiggle, start = obs.near, end = obs.far) || !angleContainedIn(angle = tile.center - centerWiggle, start = obs.near, end = obs.far))

                    val mainVisA = (distCenter && (distFar || closeNear))
                    val mainVisB = (closeCenter && (closeNear || closeFar))
                    visible = (mainVisA || mainVisB) && (opacity > 0.0f || center)
                    if (! visible) {
                        break
                    }
                }

                if ((nonvisOcclude && !visible) || opacity > 0.0f) {
                    var irrelevant = false
                    for (obs in obstructions) {
                        if (tile.a >= obs.near && tile.b <= obs.far && obs.opacity >= 1.0f) {
                            irrelevant = true
                            break
                        }
                    }
                    if (! irrelevant) {
                        obstructions.add(Obstruction(near = tile.a, far = tile.c, opacity = opacity))
                    }
                }

                out.setAtShadowCoord(tile.x, tile.y, 0, if (visible) { 1.0f } else { 0.0f })
            }
        }
    }
}