package arx.core

import arx.application.Application
import arx.core.shadowcasting.RPAS2dShadowcaster
import arx.display.components.CameraComponent
import arx.display.components.CameraID
import arx.display.components.Cameras
import arx.display.components.get
import arx.display.core.*
import arx.display.windowing.WidgetMouseReleaseEvent
import arx.display.windowing.WindowingSystem
import arx.display.windowing.WindowingSystemComponent
import arx.display.windowing.onEventDo
import arx.engine.*
import arx.engine.Event
import kotlin.math.*


interface Shadowcaster {
    fun shadowcast(out: ShadowGrid, origin: Vec3i, radius: Int)

    /**
     * shadowcast implementation in which the given function will be called with
     * all cells that are at least slightly visible. Coordinates for out function
     * are in relative shadow coords. That is, -radius .. radius
     */
    fun shadowcast(origin: Vec3i, radius: Int, out : (Int,Int,Int,Float) -> Unit)
}

interface ShadowView {
    fun shadowsAtWorldCoord(x: Int, y: Int, z: Int): FiniteGrid3Df
}

@Suppress("NOTHING_TO_INLINE")
class ShadowGrid : ShadowView {
    var origin: Vec3i = Vec3i(0, 0, 0)
    var radius: Int = 0
    var resolution : Vec3i = Vec3i(1,1,1)
    internal var nonStandardResolution = false
    internal var returnGrid : FiniteGrid3Df = FiniteGrid3Df(Vec3i(1,1,1))

    //    var resolution : Vec3i = Vec3i(1,1,1)
    var raw: FiniteGrid3Df = FiniteGrid3Df(Vec3i(0, 0, 0))
    val dimensions: Vec3i by raw::dimensions

    fun shadowAtWorldCoord(x: Int, y: Int, z: Int): Float {
        return if (nonStandardResolution) {
            val sx = (x - origin.x) * resolution.x + radius
            val sy = (y - origin.y) * resolution.y + radius
            val sz = (z - origin.z) * resolution.z + radius
            raw[sx, sy, sz]
        } else {
            val sx = x - origin.x + radius
            val sy = y - origin.y + radius
            val sz = z - origin.z + radius
            raw[sx, sy, sz]
        }
    }

    /**
     * Note that the returned ShadowBlock is still owned by the grid and should not be kept
     * or re-used, it is for efficient access only
     */
    override fun shadowsAtWorldCoord(x: Int, y: Int, z: Int): FiniteGrid3Df {
        if (nonStandardResolution) {
            val sx = (x - origin.x) * resolution.x + radius
            val sy = (y - origin.y) * resolution.y + radius
            val sz = (z - origin.z) * resolution.z + radius
            for (dx in 0 until resolution.x) {
                for (dy in 0 until resolution.y) {
                    for (dz in 0 until resolution.z) {
                        returnGrid[dx,dy,dz] = raw[sx + dx, sy + dy, sz + dz]
                    }
                }
            }
        } else {
            returnGrid[0,0,0] = shadowAtWorldCoord(x,y,z)
        }
        return returnGrid
    }

    fun shadowAtShadowCoord(x: Int, y: Int, z: Int): Float {
        return raw[x + radius, y + radius, z + radius]
    }

    fun setAtShadowCoord(x: Int, y: Int, z: Int, f: Float) {
        raw[x + radius, y + radius, z + radius] = f
    }

    fun setAtShadowCoordIfLess(x: Int, y: Int, z: Int, f: Float) {
        if (raw[x + radius, y + radius, z + radius] < f) {
            raw[x + radius, y + radius, z + radius] = f
        }
    }

    fun init(origin: Vec3i, radius: Int, resolution : Vec3i = Vec3i(1,1,1)) {
        this.origin = origin
        this.radius = radius
        if (this.resolution != resolution) {
            this.resolution = resolution
            this.returnGrid = FiniteGrid3Df(resolution)
        }
        this.nonStandardResolution = resolution.x != 1 || resolution.y != 1 || resolution.z != 1
        val expectedDims = Vec3i(radius * 2 + 1, radius * 2 + 1, radius * 2 + 1)
        if (raw.dimensions != expectedDims) {
            raw = FiniteGrid3Df(expectedDims)
        }
        raw.setAll(0.0f)
    }
}

class MergedShadowGrid internal constructor(val grids : List<ShadowGrid>) : ShadowView {
    companion object {
        operator fun invoke(grids : List<ShadowGrid>) : ShadowView {
            return if (grids.isEmpty()) {
                EmptyShadowGrid
            } else if (grids.size == 1) {
                grids[0]
            } else {
                val chosenGrids = mutableListOf<ShadowGrid>()
                val res = grids[0].resolution
                for (grid in grids) {
                    if (grid.resolution == res) {
                        chosenGrids.add(grid)
                    } else {
                        Noto.err("Tried to merge grid with differing resolution : primary resolution $res, grid res ${grid.resolution}")
                    }
                }
                MergedShadowGrid(chosenGrids)
            }
        }
    }

    val resolution = grids[0].resolution
    internal val size = resolution.x * resolution.y * resolution.z
    val returnGrid = FiniteGrid3Df(resolution)
    /**
     * Note that the returned ShadowBlock is still owned by the grid and should not be kept
     * or re-used, it is for efficient access only
     */
    override fun shadowsAtWorldCoord(x: Int, y: Int, z: Int): FiniteGrid3Df {
        returnGrid.setAll(0.0f)

        for (grid in grids) {
            val sub = grid.shadowsAtWorldCoord(x,y,z)
            for (i in 0 until size) {
                val sr = sub.getRaw(i)
                if (returnGrid.getRaw(i) < sr) {
                    returnGrid.setRaw(i, sr)
                }
            }
        }
        return returnGrid
    }
}

object EmptyShadowGrid : ShadowView {
    val returnGrid = FiniteGrid3Df(Vec3i(1,1,1))
    override fun shadowsAtWorldCoord(x: Int, y: Int, z: Int): FiniteGrid3Df {
        return returnGrid
    }
}


class OrderedSphericalIterator(radius: Int) : Iterator<Vec3i> {
    private val r2 = radius * radius

    private val Q = ShortRingBuffer(radius * 2 * 3 * 4) // 2 pi r * 3 components + fudge factor

    private val v = Vec3i(0, 0, 0)

    init {
        addV(0,0,0)
    }

    fun addV(x: Int, y: Int, z: Int) {
        if (x * x + y * y + z * z <= r2) {
            Q.enqueue(x.toShort())
            Q.enqueue(y.toShort())
            Q.enqueue(z.toShort())
        }
    }

    fun addSuccessors(dx: Int, dy: Int, dz: Int) {
        if (dx == 0 && dy == 0 && dz == 0) {
            addV(dx + 1, dy, dz); addV(dx, dy + 1, dz); addV(dx - 1, dy, dz); addV(dx, dy - 1, dz); addV(dx, dy, dz + 1); addV(dx, dy, dz - 1)
        } else if (dx == 0 && dy == 0 && dz < 0) {
            addV(dx + 1, dy, dz); addV(dx, dy + 1, dz); addV(dx - 1, dy, dz); addV(dx, dy - 1, dz); addV(dx, dy, dz - 1)
        } else if (dx == 0 && dy == 0 && dz > 0) {
            addV(dx + 1, dy, dz); addV(dx, dy + 1, dz); addV(dx - 1, dy, dz); addV(dx, dy - 1, dz); addV(dx, dy, dz + 1)
        } else if (dx > 0 && dy >= 0) {
            addV(dx, dy + 1, dz)
        } else if (dx <= 0 && dy > 0) {
            addV(dx - 1, dy, dz)
        } else if (dx < 0 && dy <= 0) {
            addV(dx, dy - 1, dz)
        } else if (dx >= 0 && dy < 0) {
            addV(dx + 1, dy, dz)
        }

        if (dx == 0 && dy > 0) {
            addV(dx, dy + 1, dz)
        } else if (dx == 0 && dy < 0) {
            addV(dx, dy - 1, dz)
        } else if (dx > 0 && dy == 0) {
            addV(dx + 1, dy, dz)
        } else if (dx < 0 && dy == 0) {
            addV(dx - 1, dy, dz)
        }
    }

    override fun hasNext(): Boolean {
        return Q.size > 0
    }

    override fun next(): Vec3i {
        v(Q.dequeue().toInt(), Q.dequeue().toInt(), Q.dequeue().toInt())

        addSuccessors(v.x, v.y, v.z)

        return v
    }
}


object ShadowcastTestComponent : DisplayComponent() {

    val img = Image.ofSize(64, 64).withPixels { x, y, v ->
        v(x * 4, y * 4, 0, 255)
    }

    val shadowGrid = ShadowGrid()

    val imgWidget = initWithWorld { this[WindowingSystem].createWidget("ShadowcastTestWidgets.ImageDisplay") }


    val origin = Vec3i(32,32,0)
    var obstructions = mapOf(
        Vec3i(36, 32, 0) to 1.0f,
//                Vec3i(37, 34, 0) to 1.0f,
        Vec3i(37, 35, 0) to 1.0f,
        Vec3i(37, 36, 0) to 1.0f,
        Vec3i(37, 37, 0) to 1.0f,
    )

    fun World.updateShadowcast() {
        val v = Vec3i(0, 0, 0)
        val caster = RPAS2dShadowcaster(
            opacityFn = { x, y, z ->
                v(x, y, z)
                obstructions[v] ?: 0.0f
            },
            filterFn = { x, y, z ->
                z == 0
            }
        )

//        caster.shadowcast(shadowGrid, origin, 30)

        Metrics.timer("RCAS").time {
            caster.shadowcast(shadowGrid, origin, 30)
        }

        Metrics.print()

        img.withPixels { x, y, c ->
            val s = shadowGrid.shadowAtWorldCoord(x, y, 0)
            val si = (s * 255).toInt()
            if ((obstructions[Vec3i(x, y, 0)] ?: 0.0f) > 0.0f) {
                c(si, 0, 0, 255)
            } else if (Vec3i(x,y,0) == origin) {
                c(0, si, 0, 255)
            } else {
                c(si, si, si, 255)
            }
        }
        img.revision++

        imgWidget().bind("image", img)
    }

    override fun initialize(world: World) {
        with(world) {
            imgWidget().onEventDo<WidgetMouseReleaseEvent> { event ->
                val p = event.position - Vec2f(event.widget.resClientX.toFloat(), event.widget.resClientY.toFloat())
                val pf = Vec2f((p.x / event.widget.resClientWidth).clamp(0.0f, 1.0f), (p.y / event.widget.resClientHeight).clamp(0.0f, 1.0f))

                val v = Vec3i((pf.x * 64f).toInt(), ((1.0f - pf.y) * 64f).toInt(), 0)
                if (obstructions.containsKey(v)) {
                    obstructions -= v
                } else {
                    obstructions += v to 1.0f
                }
                updateShadowcast()
                imgWidget().markForFullUpdateAndAllDescendants()
            }

            updateShadowcast()
        }
    }

    override fun handleEvent(world: World, event: Event) {
        super.handleEvent(world, event)
    }
}



object Shadowcast3DCamera : CameraID {
    override val startingCamera = PixelCamera().apply { origin = Vec3f(0.0f, -8.0f, 0.0f); scaleIncrement = 32.0f; speed = Vec3f(8.0f, 8.0f, 8.0f); scale = 3.0f }
}

object ShadowcastTestComponent3D : DisplayComponent() {

    val shadowGrid = ShadowGrid()

    val origin = Vec3i(16,16,4)
    var obstructions = FiniteGrid3Df(Vec3i(64,64,10))

    val vao = VAO(MinimalVertex())
    val tb = TextureBlock(1024)
    val shader = Resources.shader("arx/shaders/minimal")

    val visionRange = 30


    fun renderQuad(p : Vec3f, img : Image, drawColor: RGBA) {
        val tc = tb.getOrUpdate(img)
        for (q in 0 until 4) {
            vao.addV().apply {
                vertex = p + CenteredUnitSquare3D[q]
                texCoord = tc[q]
                color = drawColor
            }
        }
        vao.addIQuad()
    }

    fun tv(v : Int) : Int {
        return if (v < 0) {
            (v - 1) / 2
        } else {
            (v) / 2
        }
    }

    fun World.updateShadowcast() {
        val caster = RPAS3dShadowcaster(
            opacityFn = { x, y, z -> obstructions[x, y, z] },
            filterFn = { x, y, z -> z >= 0 && z <= 10 },
            resolution = Vec3i(2,2,1)
        )

        Metrics.timer("RCAS").time {
            caster.shadowcast(shadowGrid, origin, visionRange)
        }
        Metrics.print("RCAS")


        val obstructionImage = Resources.image("display/images/stone.png")
        val obstructionImage2 = Resources.image("display/images/grass.png")
        val characterImage = Resources.image("display/images/character.png")

        vao.reset()


        val p = Vec3f()
        val drawRadius = 20
        for (wx in origin.x + drawRadius downTo origin.x - drawRadius) {
            for (wy in origin.y + drawRadius downTo origin.y - drawRadius) {
                for (wz in 0 until 10) {
                    val shadows = shadowGrid.shadowsAtWorldCoord(wx, wy, wz)

                    var maxShadow = 0.0f
                    var avgShadow = 0.0f
                    var inVision = 0
                    for (dx in 0 .. 1) {
                        for (dy in 0 .. 1) {
                            val shadow = shadows[dx,dy,0]
                            maxShadow = maxShadow.max(shadow)
                            avgShadow += shadow
                            if (shadow > 0.5f) {
                                inVision++
                            }
                        }
                    }

                    avgShadow /= 4.0f
                    if (obstructions[wx, wy, wz - (wz - origin.z).sign] > 0.0f) {

                    } else {
                        maxShadow = avgShadow
                    }

                    IsoCoord.project(wx, wy, wz, p)

                    if (obstructions[wx, wy, wz] > 0.0f) {
                        if (wz >= 7) {
                            renderQuad(p, obstructionImage2, RGBAf(maxShadow, maxShadow, maxShadow, 1.0f))
                        } else {
                            renderQuad(p, obstructionImage, RGBAf(maxShadow, maxShadow, maxShadow, 1.0f))
                        }
                    }

                    if (wx == origin.x && wy == origin.y && wz == origin.z) {
                        renderQuad(p + Vec3f(0.0f, -0.2f, 0.0f), characterImage, White)
                    }
                }
            }
        }

//        val p = Vec3f()
//        for (dx in visionRange downTo -visionRange) {
//            val wx = origin.x + tv(dx)
//            for (dy in visionRange downTo -visionRange) {
//                val wy = origin.y + tv(dy)
//                for (dz in -10 .. 10) {
//                    val wz = origin.z + dz
//                    val shadow = shadowGrid.shadowAtShadowCoord(dx, dy, dz)
//
//                    IsoCoord.project(wx, wy, wz, p)
//
//                    if (obstructions[wx, wy, wz] > 0.0f) {
//                        if (shadow > 0.5f) {
//                            if (dz >= 3) {
//                                renderQuad(p, obstructionImage2, White)
//                            } else {
//                                renderQuad(p, obstructionImage, White)
//                            }
//                        } else {
//                            renderQuad(p, obstructionImage, RGBAf(0.5f, 0.5f, 0.5f, 1f))
//                        }
//                    }
//
//                    if (dx == 0 && dy == 0 && dz == 0) {
//                        renderQuad(p + Vec3f(0.0f, -0.2f, 0.0f), characterImage, White)
//                    }
//                }
//            }
//        }
    }

    override fun initialize(world: World) {
        with(world) {

            for (x in 0 until obstructions.dimensions.x) {
                for (y in 0 until obstructions.dimensions.y) {
                    for (z in 0 .. 3) {
                        obstructions[x, y, z] = 1.0f
                    }
                }
            }

            for (x in 0 until 10) {
                for (y in 0 until 10) {
//                    obstructions[origin.x - 3 - x, origin.y - 3 - y, origin.z + 5] = 1.0f
                    obstructions[origin.x - 6 - x/2, origin.y - 6 - y/2, origin.z + 3] = 1.0f

                }
            }

            for (x in 0 until 15) {
                for (z in 0 until 5) {
                    obstructions[origin.x - 10, origin.y + x, origin.z + z] = 1.0f
                }
            }
            obstructions[origin.x + 4, origin.y + 1, origin.z] = 1.0f
            obstructions[origin.x + 4, origin.y + 1, origin.z + 1] = 1.0f

            obstructions[origin.x + 3, origin.y + 3, origin.z] = 1.0f

            for (z in 0 until 4) {
                obstructions[origin.x + 5, origin.y + 5, origin.z + z] = 1.0f
                obstructions[origin.x + 6, origin.y + 5, origin.z + z] = 1.0f
            }

            obstructions[origin.x, origin.y - 1, origin.z] = 1.0f

            for (t in 2 until 8) {
                obstructions[origin.x - 4, origin.y + t, origin.z] = 1.0f
            }
            origin.z += 1

            updateShadowcast()
        }
    }

    override fun handleEvent(world: World, event: Event) {
        when (event) {
            is KeyReleaseEvent -> {
                when (event.key) {
                    Key.Left -> origin.x -= 1
                    Key.Right -> origin.x += 1
                    Key.Up -> origin.y += 1
                    Key.Down -> origin.y -= 1
                    Key.LeftBracket -> origin.z -= 1
                    Key.RightBracket -> origin.z += 1
                    else -> {}
                }
                with(world) {
                    updateShadowcast()
                }
            }
        }
    }

    override fun update(world: World): Boolean {
        return true
    }

    override fun draw(world: World) {
        shader.bind()
        tb.bind()

        val matrix = world[Cameras][Shadowcast3DCamera].run { projectionMatrix() * modelviewMatrix() }
        shader.setUniform("Matrix", matrix)

        vao.sync()
        vao.draw()
    }
}

fun main() {
    Application()
        .run(
            Engine(
                mutableListOf(),
                mutableListOf(ShadowcastTestComponent3D, CameraComponent, WindowingSystemComponent)
            )
        )
}