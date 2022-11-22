package arx.core

import arx.core.shadowcasting.RPAS2dShadowcaster
import arx.display.core.Image
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.math.PI
import kotlin.math.atan2

internal class ShadowcastingTest {


    @Test
    internal fun testAngles() {
        val origin = Vec3f(0.5f,0.5f,0.5f)
        for (r in 1 until 4) {
            val rf = r.toFloat()
            for (x in 0 .. r) {

                val angleAlloc = 1f / (rf + 1.0f)

                val hNear = x * angleAlloc
                val hCenter = hNear + 0.5f * angleAlloc
                val hFar = hNear + angleAlloc


                val dest = Vec3f(rf + origin.x, x.toFloat() + origin.y, origin.z)

                val hNear2 = atan2((dest.y - 0.5f), (dest.x) - 0.5f) / (PI / 4.0f)
//                val hCenter2 = atan2((dest.y), (dest.x)) / (PI / 4.0f)
                val hFar2 = atan2((dest.y + 0.5f), (dest.x) - 0.5f) / (PI / 4.0f)

                val hSide2 = atan2((dest.y - 0.5f), (dest.x + 1.0f) - 0.5f) / (PI / 4.0f)

                print(" $x : ${(hSide2 * 100).toInt()} - ${(hNear2 * 100).toInt()} - ${(hFar2 * 100).toInt()} ||")
            }
            println()
            println("-------------------")
        }
    }


    @Test
    internal fun benchmarkRPASV2d() {
        val fewObstructions = FiniteGrid3Df(Vec3i(64,64,8))
        val manyObstructions = FiniteGrid3Df(Vec3i(64,64,8))

        val rand = Random(1337)
        for (i in 0 until 64) {
            fewObstructions[rand.nextInt(64), rand.nextInt(64), 0] = 1.0f
        }
        for (i in 0 until 256) {
            manyObstructions[rand.nextInt(64), rand.nextInt(64), 0] = 1.0f
        }

        val fewObsShadowcaster = RPAS2dShadowcaster({ x, y, z -> fewObstructions[x,y,z]},{ x, y, z -> z == 0})
        val manyObsShadowcaster = RPAS2dShadowcaster({ x, y, z -> manyObstructions[x,y,z]},{ x, y, z -> z == 0})

        val out = ShadowGrid()

        val testCases : Map<String, (RPAS2dShadowcaster) -> Unit> = mapOf(
            "RPAS" to { s -> s.shadowcastOriginal(out, Vec3i(32,32,0), 30) },
            "RPASV2d" to { s -> s.shadowcastVariant(out, Vec3i(32,32,0), 30) }
        )

        for ((k,fn) in testCases) {

            for (i in 0 until 100) {
                Metrics.timer("$k few obstructions").time {
                    fn(fewObsShadowcaster)
                }
                Metrics.timer("$k many obstructions").time {
                    fn(manyObsShadowcaster)
                }

                if (i == 0) {
                    val img = Image.ofSize(64, 64).withPixels { x, y, v ->
                        val si = (out.shadowAtWorldCoord(x,y,0) * 255).toInt()
                        if (manyObstructions[x,y,0] > 0.0f) {
                            v(si,0,0,255)
                        } else {
                            v(si,si,si,255)
                        }
                    }
                    img.writeToFile("/tmp/$k.png")
                }
            }
        }

        Metrics.print("RPAS")
    }

    @Test
    internal fun benchmarkRPASV3d() {
        val fewObstructions = FiniteGrid3Df(Vec3i(64,64,8))
        val manyObstructions = FiniteGrid3Df(Vec3i(64,64,8))

        for (x in 0 until 64) {
            for (y in 0 until 64) {
                for (z in 0 .. 3) {
                    fewObstructions[x, y, z] = 1.0f
                    manyObstructions[x, y, z] = 1.0f
                }
            }
        }

        var rand = Random(1337)
        for (i in 0 until 64) {
            fewObstructions[rand.nextInt(64), rand.nextInt(64), rand.nextInt(4) + 4] = 1.0f
        }
        rand = Random(1337)
        for (i in 0 until 256) {
            manyObstructions[rand.nextInt(64), rand.nextInt(64), rand.nextInt(4) + 4] = 1.0f
        }

        val fewObsShadowcaster = RPAS3dShadowcaster({ x, y, z -> fewObstructions[x,y,z]},{ x, y, z -> z >= 0 && z < 8}, Vec3i(1,1,1))
        val manyObsShadowcaster = RPAS3dShadowcaster({ x, y, z -> manyObstructions[x,y,z]},{ x, y, z -> z >= 0 && z < 8}, Vec3i(1,1,1))

        val out = ShadowGrid()

        val testCases : Map<String, (Shadowcaster) -> Unit> = mapOf(
            "RPAS3d" to { s -> s.shadowcast(out, Vec3i(32,32,4), 20) },
        )

        for ((k,fn) in testCases) {

            for (i in 0 until 200) {
                Metrics.timer("$k few obstructions").time {
                    fn(fewObsShadowcaster)
                }
                Metrics.timer("$k many obstructions").time {
                    fn(manyObsShadowcaster)
                }

                if (i == 0) {
                    val img = Image.ofSize(64, 64).withPixels { x, y, v ->
                        val si = (out.shadowAtWorldCoord(x,y,4) * 255).toInt()
                        if (manyObstructions[x,y,4] > 0.0f) {
                            v(si,0,0,255)
                        } else {
                            v(si,si,si,255)
                        }
                    }
                    img.writeToFile("/tmp/$k.png")
                }
            }
        }

        Metrics.print("RPAS")
    }


    @Test
    internal fun benchmarkRPASV3dBits() {
        val fewObstructions = FiniteGrid3Dbits(Vec3i(64,64,8))
        val manyObstructions = FiniteGrid3Dbits(Vec3i(64,64,8))

        for (x in 0 until 64) {
            for (y in 0 until 64) {
                for (z in 0 .. 3) {
                    fewObstructions[x, y, z] = true
                    manyObstructions[x, y, z] = true
                }
            }
        }

        var rand = Random(1337)
        for (i in 0 until 64) {
            fewObstructions[rand.nextInt(64), rand.nextInt(64), rand.nextInt(4) + 4] = true
        }
        rand = Random(1337)
        for (i in 0 until 256) {
            manyObstructions[rand.nextInt(64), rand.nextInt(64), rand.nextInt(4) + 4] = true
        }

        val fewObsShadowcaster = RPAS3dShadowcaster({ x, y, z -> if (fewObstructions[x,y,z]) { 1.0f } else { 0.0f } },{ x, y, z -> z >= 0 && z < 8}, Vec3i(1,1,1))
        val manyObsShadowcaster = RPAS3dShadowcaster({ x, y, z -> if (manyObstructions[x,y,z]) { 1.0f } else { 0.0f } },{ x, y, z -> z >= 0 && z < 8}, Vec3i(1,1,1))

        val outBits = FiniteGrid3Dbits(Vec3i(66,66,8))

        val testCases : Map<String, (Shadowcaster) -> Unit> = mapOf(
            "RPAS3d" to { s ->
                outBits.setAll(false)
                s.shadowcast(Vec3i(32,32,4), 20) { x,y,z,v -> if (v > 0.0f) { outBits[32 + x, 32 + y, 4 + z] = true } }
            },
        )

        for ((k,fn) in testCases) {

            for (i in 0 until 200) {
                Metrics.timer("$k few obstructions").time {
                    fn(fewObsShadowcaster)
                }
                Metrics.timer("$k many obstructions").time {
                    fn(manyObsShadowcaster)
                }

                if (i == 0) {
                    val img = Image.ofSize(64, 64).withPixels { x, y, v ->
                        val si = if (outBits[x,y,4]) { 255 } else { 0 }
                        if (manyObstructions[x,y,4]) {
                            v(si,0,0,255)
                        } else {
                            v(si,si,si,255)
                        }
                    }
                    img.writeToFile("/tmp/$k.png")
                }
            }
        }

        Metrics.print("RPAS")
    }
}