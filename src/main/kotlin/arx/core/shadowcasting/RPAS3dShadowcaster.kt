package arx.core

import it.unimi.dsi.fastutil.floats.FloatArrayList
import kotlin.math.PI
import kotlin.math.atan2

class SphereItem(var x : Int, var y : Int, var z : Int, var r : Int, val hAngles : FloatArray = FloatArray(7), val vAngles : FloatArray = FloatArray(7))

internal data class Octant3D(val qx : Int, val qy : Int, val qz : Int, val vert : Boolean, val top : Boolean, val rVector : Vec3i) {
    companion object {
        fun allOctants() : Iterator<Octant3D> {
            return iterator {
                for (x in listOf(1, -1)) {
                    for (y in listOf(1, -1)) {
                        for (z in listOf(1, -1)) {
                            for (vert in arrayOf(true, false)) {
                                for (top in arrayOf(true, false)) {
                                    yield(Octant3D(x, y, z, vert, top, rVector(x,y,z,vert,top)))
                                }
                            }
                        }
                    }
                }
            }
        }

        fun rVector(qx : Int, qy: Int, qz : Int, vert: Boolean, top: Boolean) : Vec3i {
            return if (vert) {
                if (top) {
                    Vec3i(0,0,qz)
                } else {
                    Vec3i(0,qy,0)
                }
            } else {
                if (top) {
                    Vec3i(0,0,qz)
                } else {
                    Vec3i(qx,0,0)
                }
            }
        }

        const val NL = 0
        const val NH = 1
        const val FL = 2
        const val FH = 3
        const val C = 4
        const val NC = 5
        const val LC = 6


        const val P4 = PI / 4.0f

        // x, y, z[0,8]
        val angles = Array(31) { r ->
            FloatArray((r + 1) * 7)
        }

        init {
            fun calc(x : Float, y : Float) : Float {
                return (atan2(y, x) / P4).toFloat()
            }

            for (r in 1 .. 30) {
                val rf = r.toFloat()
                for (y in 0 .. r) {
                    val yi = y * 7
                    angles[r][yi + NL] = calc(rf - 0.5f, y - 0.5f)
                    angles[r][yi + NH] = calc(rf - 0.5f, y + 0.5f)
                    angles[r][yi + FL] = calc(rf + 0.5f, y - 0.5f)
                    angles[r][yi + FH] = calc(rf + 0.5f, y + 0.5f)
                    angles[r][yi + C] = calc(rf, y.toFloat())
                    angles[r][yi + NC] = calc(rf - 0.5f, y.toFloat())
                    angles[r][yi + LC] = calc(rf, y - 0.5f)
//                        bAngles[r][x] = atan2(x - 0.5f, rf - 0.5f) / (PI / 4.0f).toFloat() // close near corner
//                        cAngles[r][x] = atan2(x + 0.5f, rf - 0.5f) / (PI / 4.0f).toFloat() // close far corner
//                        aAngles[r][x] = atan2(x - 0.5f, rf + 0.5f) / (PI / 4.0f).toFloat() // dist near corner
//                        centerAngles[r][x] = atan2(x.toFloat(), rf) / (PI / 4.0f).toFloat() // dist near corner
                }
            }
        }
    }

    fun cells(maxR : Int) : Iterator<SphereItem> {
        return iterator {
            val v = SphereItem(0, 0, 0, 0)
            yield(v)

            val maxr2 = maxR * maxR
            for (r in 1 .. maxR) {
                v.r = r
                val rAngles = angles[r]
                for (ha in 0 .. r) {
                    val hi = ha * 7
                    for (va in 0 .. r) {
                        val vi = va * 7

                        if (vert) {
                            if (top) {
                                v.x = ha * qx
                                v.y = va * qy
                                v.z = r * qz
                            } else {
                                v.x = ha * qx
                                v.y = r * qy
                                v.z = va * qz
                            }
                        } else {
                            if (top) {
                                v.x = va * qx
                                v.y = ha * qy
                                v.z = r * qz
                            } else {
                                v.x = r * qx
                                v.y = ha * qy
                                v.z = va * qz
                            }
                        }

                        if (v.x * v.x + v.y * v.y + v.z * v.z > maxr2) {
                            continue
                        }

                        v.hAngles[NL] = rAngles[hi + NL]
                        v.hAngles[NH] = rAngles[hi + NH]
                        v.hAngles[FL] = rAngles[hi + FL]
                        v.hAngles[FH] = rAngles[hi + FH]
                        v.hAngles[C] = rAngles[hi + C]
                        v.hAngles[NC] = rAngles[hi + NC]
                        v.hAngles[LC] = rAngles[hi + LC]

                        v.vAngles[NL] = rAngles[vi + NL]
                        v.vAngles[NH] = rAngles[vi + NH]
                        v.vAngles[FL] = rAngles[vi + FL]
                        v.vAngles[FH] = rAngles[vi + FH]
                        v.vAngles[C] = rAngles[vi + C]
                        v.vAngles[NC] = rAngles[vi + NC]
                        v.vAngles[LC] = rAngles[vi + LC]

                        yield(v)
                    }
                }
            }
        }
    }
}

class RPAS3dShadowcaster(
    val opacityFn: (Int, Int, Int) -> Float, // the opacity of a given world coordinate
    val filterFn: (Int, Int, Int) -> Boolean, // filter for deciding whether the given coordinate should be considered, input in world coordinates
    val resolution : Vec3i,
) : Shadowcaster {

    fun angleContainedIn(angle : Float, start: Float, end : Float) : Boolean {
        return angle > start && angle < end
    }

    internal inline fun maxOf(a : Float, b : Float, c : Float, d : Float) : Float {
        return a.max(b).max(c).max(d)
    }
    internal inline fun minOf(a : Float, b : Float, c : Float, d : Float) : Float {
        return a.min(b).min(c).min(d)
    }
    internal inline fun toWorldOffset(v : Int, r: Int) : Int {
        return if (r != 1) {
            if (v < 0) {
                (v - (r - 1)) / r
            } else {
                v / r
            }
        } else {
            v
        }
    }
    
    override fun shadowcast(out: ShadowGrid, origin: Vec3i, radius: Int) {
        out.init(origin, radius + 1, resolution)

        shadowcast(origin, radius) { x, y, z, v ->
            out.setAtShadowCoordIfLess(x,y,z,v)
        }
    }

    override fun shadowcast(origin: Vec3i, radius: Int, out : (Int,Int,Int,Float) -> Unit) {
        val NBLC = 0
        val NBRC = 1
        val NTLC = 2
        val NTRC = 3
        val FBLC = 4
        val FBRC = 5
        val FTLC = 6
        val NCC = 7
        val BCC = 8
        val LCC = 9

        val pointIndexes = arrayOf(Vec2i(Octant3D.NL, Octant3D.NL), Vec2i(Octant3D.NH, Octant3D.NL), Vec2i(
            Octant3D.NL,
            Octant3D.NH
        ), Vec2i(Octant3D.NH, Octant3D.NH), Vec2i(Octant3D.FL, Octant3D.FL), Vec2i(Octant3D.FH, Octant3D.FL), Vec2i(
            Octant3D.FL,
            Octant3D.FH
        ), Vec2i(Octant3D.NC, Octant3D.NC), Vec2i(Octant3D.C, Octant3D.LC), Vec2i(Octant3D.LC, Octant3D.C))

        val faces = arrayOf(arrayOf(NBLC, NBRC, NTLC, NTRC, NCC), arrayOf(NBLC, NBRC, FBLC, FBRC, BCC), arrayOf(NBLC, NTLC, FTLC, FBLC, LCC))

        var totalObstructions = 0

        val flatObstructions = FloatArrayList(128)
        for (octant in Octant3D.allOctants()) {
            flatObstructions.clear()
            val pvis = BooleanArray(10) { true }
            val pH = FloatArray(10)
            val pV = FloatArray(10)

            var fullyObstructed = false
            var visibleInSlice = 1
            var lastR = 0

            val oro = Vec3i(if (octant.qx > 0) { octant.qx * resolution.x - 1 } else { 0 },
                            if (octant.qy > 0) { octant.qy * resolution.y - 1 } else { 0 },
                            if (octant.qz > 0) { octant.qz * resolution.z - 1 } else { 0 })

            for (cell in octant.cells(radius)) {
                val cx = cell.x + oro.x
                val cy = cell.y + oro.y
                val cz = cell.z + oro.z

                if (fullyObstructed) {
                    break
                }

                if (cell.r != lastR) {
                    if (visibleInSlice == 0) {
                        fullyObstructed = true
                    }
                    visibleInSlice = 0
                    lastR = cell.r
                }

                var centerVisible = true

                val wx = origin.x + toWorldOffset(cx, resolution.x)
                val wy = origin.y + toWorldOffset(cy, resolution.y)
                val wz = origin.z + toWorldOffset(cz, resolution.z)


                val opacity = opacityFn(wx, wy, wz)
                if (! filterFn(wx, wy, wz)) {
                    continue
                }

                if (flatObstructions.isNotEmpty()) {
                    for (pi in 0 until 10) {
                        pvis[pi] = true
                        pH[pi] = cell.hAngles[pointIndexes[pi].x]
                        pV[pi] = cell.vAngles[pointIndexes[pi].y]
                    }
                }


                var i = 0
                while (i < flatObstructions.size) {
                    val hNear = flatObstructions.getFloat(i)
                    val hFar = flatObstructions.getFloat(i + 1)
                    val vNear = flatObstructions.getFloat(i + 2)
                    val vFar = flatObstructions.getFloat(i + 3)
                    i += 4
                    if (opacity > 0.0f) {
                        for (pi in 0 until 10) {
                            // eliminate visibility on the point if it is contained in the H and V arcs of an obstacle
                            pvis[pi] = pvis[pi] && !(angleContainedIn(angle = pV[pi], start = vNear, end = vFar) && angleContainedIn(angle = pH[pi], start = hNear, end = hFar))
                        }
                        if (!pvis[NCC] && !pvis[BCC] && !pvis[LCC]) {
                            break
                        }
                    } else {
                        centerVisible = centerVisible && !(angleContainedIn(cell.hAngles[Octant3D.C], hNear, hFar) && angleContainedIn(cell.vAngles[Octant3D.C], vNear, vFar))
                    }
                }


                if (opacity > 0.0f) {
                    var anyFaceVisible = false
                    for (fi in 0 until 3) {
                        var pointsVisible = 0
                        // center point must be visible
                        if (! pvis[faces[fi][4]]) {
                            continue
                        }
                        for (fpi in 0 until 4) {
                            if (pvis[faces[fi][fpi]]) {
                                pointsVisible++
                            }
                        }
                        // and two more points must be visible as well
                        if (pointsVisible >= 2) {
                            anyFaceVisible = true
                            break
                        }
                    }

                    if (anyFaceVisible) {
                        out(cx, cy, cz, 1.0f)
                    }

                    val hNear = minOf(cell.hAngles[Octant3D.NL], cell.hAngles[Octant3D.NH], cell.hAngles[Octant3D.FL], cell.hAngles[Octant3D.FH])
                    val hFar = maxOf(cell.hAngles[Octant3D.NL], cell.hAngles[Octant3D.NH], cell.hAngles[Octant3D.FL], cell.hAngles[Octant3D.FH])

                    val vNear = minOf(cell.vAngles[Octant3D.NL], cell.vAngles[Octant3D.NH], cell.vAngles[Octant3D.FL], cell.vAngles[Octant3D.FH])
                    val vFar = maxOf(cell.vAngles[Octant3D.NL], cell.vAngles[Octant3D.NH], cell.vAngles[Octant3D.FL], cell.vAngles[Octant3D.FH])

                    var fullyContained = false

                    i = 0
                    while (i < flatObstructions.size) {
                        if (flatObstructions.getFloat(i) <= hNear && flatObstructions.getFloat(i+1) >= hFar && flatObstructions.getFloat(i+2) <= vNear && flatObstructions.getFloat(i+3) >= vFar) {
                            fullyContained = true
                            break
                        }
                        i += 4
                    }

                    i = 0
                    while (i < flatObstructions.size) {
                        if (flatObstructions.getFloat(i+2) == vNear && flatObstructions.getFloat(i+3) == vFar) {
                            if (flatObstructions.getFloat(i+1) >= hNear && flatObstructions.getFloat(i+1) < hFar) {
                                fullyContained = true
                                flatObstructions.set(i+1, hFar)
                                break
                            }
                        }
                        i += 4
                    }

                    if (! fullyContained) {
                        flatObstructions.add(hNear)
                        flatObstructions.add(hFar)
                        flatObstructions.add(vNear)
                        flatObstructions.add(vFar)
//                        println("Adding new $hNear, $hFar : $vNear, $vFar  (${cell.x}, ${cell.y}, ${cell.z} | ${octant.qx}, ${octant.qy}, ${octant.qz}, ${octant.top} / ${octant.vert})")
                    }
                } else {
                    if (centerVisible) {
                        visibleInSlice++
                        out(cx, cy, cz, 1.0f)
                    } else {
//                        out.setAtShadowCoordIfLess(cx, cy, cz, 0.0f)
                    }
                }
            }
            totalObstructions += flatObstructions.size / 4
        }

        Metrics.meter("RPAS - obstructions").mark(totalObstructions.toLong())
    }
}