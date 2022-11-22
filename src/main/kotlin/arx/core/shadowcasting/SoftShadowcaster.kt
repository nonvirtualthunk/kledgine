package arx.core.shadowcasting

import arx.core.*
import dev.romainguy.kotlin.math.pow
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.atan2

class SoftShadowcaster(
    val opacityFn: (Int, Int, Int) -> Float, // the opacity of a given world coordinate
    val filterFn: (Int, Int, Int) -> Boolean, // filter for deciding whether the given coordinate should be considered, input in world coordinates
//    val resolution : Vec3i = Vec3i(1,1,1), // the shadow resolution per coordinate in world, by axis
) : Shadowcaster {

    companion object {
        val scratchGrids = ConcurrentHashMap<Int, ShadowGrid>()

        fun checkOutScratchGrid(origin: Vec3i, radius: Int): ShadowGrid {
            val ret = scratchGrids.remove(radius) ?: ShadowGrid()
            ret.init(origin, radius)
            return ret
        }

        fun returnScratchGrid(grid: ShadowGrid) {
            scratchGrids[grid.radius] = grid
        }
    }

    override fun shadowcast(out: ShadowGrid, origin: Vec3i, radius: Int) {
        out.init(origin, radius)

        shadowcast(origin, radius) { x,y,z,v -> out.setAtShadowCoord(x,y,z,v) }
    }

    override fun shadowcast(origin: Vec3i, radius: Int, out: (Int, Int, Int, Float) -> Unit) {
        val scratch = checkOutScratchGrid(origin, radius)

        val dv = Vec3f(0.0f, 0.0f, 0.0f)
        val adjv = Vec3f(0.0f, 0.0f, 0.0f)
        val adjDots = FloatArray(8)
        val adjShadows = FloatArray(8)

        val iter = OrderedSphericalIterator(radius)
        while (iter.hasNext()) {
            val sc = iter.next()
            val sx = sc.x
            val sy = sc.y
            val sz = sc.z

            val sxa = sx.abs()
            val sya = sy.abs()
            val sza = sz.abs()
            val ssum = sxa + sya + sza

            val signx = Integer.signum(sx)
            val signy = Integer.signum(sy)
            val signz = Integer.signum(sz)

            val wx = origin.x + sx
            val wy = origin.y + sy
            val wz = origin.z + sz

            if (!filterFn(wx, wy, wz)) {
                continue
            }

            var srcShadow = 0.0f
            var opacity = 0.0f

            if (sx != 0 || sy != 0 || sz != 0) {
//                val pcntX = sxa / ssum.toFloat()
//                val pcntY = sya / ssum.toFloat()
//                val pcntZ = sza / ssum.toFloat()
                dv(sx.toFloat(), sy.toFloat(), sz.toFloat())
                dv.normalize()

                var dotSum = 0.0f
                var i = 0
                for (dx in 0..1) {
                    val avx = dx * signx
                    val ax = sx - avx
                    for (dy in 0..1) {
                        val avy = dy * signy
                        val ay = sy - avy
                        for (dz in 0..1) {
                            val avz = dz * signz
                            val az = sz - avz
                            if (avx != 0 || avy != 0 || avz != 0) {
                                adjv(avx.toFloat(), avy.toFloat(), avz.toFloat())
                                adjv.normalize()


                                val dot = dv.dot(adjv)
                                adjDots[i] = dot
                                dotSum += dot
                                adjShadows[i] = scratch.shadowAtShadowCoord(ax, ay, az)
                            } else {
                                adjDots[i] = 0.0f
                                adjShadows[i] = 0.0f
                            }
                            i++
                        }
                    }
                }


                // exponential dot product, somewhat harder edged than linear
                var squaredDotSum = 0.0f
                val exponent = 5.0f
                val dotThreshold = 0.75f
                for (j in 0 until 8) {
                    if (adjDots[j] > dotThreshold) {
                        squaredDotSum += pow(adjDots[j], exponent)
                    }
                }
                for (j in 0 until 8) {
                    if (adjDots[j] > dotThreshold) {
                        srcShadow += (pow(adjDots[j], exponent) / squaredDotSum) * dampShadow(adjShadows[j])
                    }
                }

                srcShadow = pow(srcShadow, 0.975f).clamp(0.0f, 1.0f)

                // linear with proportion of dot product, soft edged but shadow "bleeds" outward from obstruction
//                for (j in 0 until 8) {
//                    srcShadow += dampShadow(adjShadows[j]) * (adjDots[j] / dotSum)
//                }

                // choosing the closest vector, sharp edged and gets weird at the cutover points
//                srcShadow = adjShadows[adjDots.maxIndex()]
                opacity = opacityFn(wx, wy, wz)
            } else {
                srcShadow = 1.0f
            }

            scratch.setAtShadowCoord(sx, sy, sz, srcShadow * (1.0f - opacity))
            out(sx, sy, sz, srcShadow)
        }
    }


    fun dampShadow(f : Float) : Float {
        return if (f < 0.5f) {
            f
        } else {
            java.lang.Float.min(f + 0.02f, 1.0f)
        }
    }

}