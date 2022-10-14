package arx.display.windowing

import arx.application.Application
import arx.core.*
import arx.core.RGBA
import arx.display.core.*
import arx.engine.DisplayComponent
import arx.engine.DisplayEvent
import arx.engine.Event
import arx.engine.World
import dev.romainguy.kotlin.math.ortho
import org.lwjgl.opengl.GL11

class WindowingSystemVertex : VertexDefinition() {
    private val vertexOffset = offsetFor(WindowingSystemVertex::vertex)
    private val colorOffset = offsetFor(WindowingSystemVertex::color)
    private val texCoordOffset = offsetFor(WindowingSystemVertex::texCoord)
    private val boundsMinOffset = offsetFor(WindowingSystemVertex::boundsMin)
    private val boundsMaxOffset = offsetFor(WindowingSystemVertex::boundsMax)

    @Attribute(location = 0)
    var vertex: Vec3i
        get() {
            return getInternal(vertexOffset, Vec3i())
        }
        set(v) {
            setInternal(vertexOffset, v)
        }

    @Attribute(location = 1)
    @Normalize
    var color: RGBA
        get() {
            return getInternal(colorOffset, RGBA())
        }
        set(v) {
            setInternal(colorOffset, v)
        }

    @Attribute(location = 2)
    var texCoord: Vec2f
        get() {
            return getInternal(texCoordOffset, Vec2f())
        }
        set(v) {
            setInternal(texCoordOffset, v)
        }

    @Attribute(location = 3)
    var boundsMin: Vec2i
        get() {
            return getInternal(boundsMinOffset, Vec2i())
        }
        set(v) {
            setInternal(boundsMinOffset, v)
        }

    @Attribute(location = 4)
    var boundsMax: Vec2i
        get() {
            return getInternal(boundsMaxOffset, Vec2i())
        }
        set(v) {
            setInternal(boundsMaxOffset, v)
        }

    override fun toString(): String {
        return "WindowingSystemVertex(vertex=$vertex, color=$color, texCoord=$texCoord, boundsMin=$boundsMin, boundsMax=$boundsMax)"
    }


}



object WindowingSystemComponent : DisplayComponent() {
    val vao = VAO(WindowingSystemVertex())
    val textureBlock = TextureBlock(4096)

    var needsRerender = true

    val shader = Resources.shader("arx/shaders/windowing")

    override fun initialize(world: World) {
        world[WindowingSystem].registerStandardComponents()
    }

    override fun update(world: World) : Boolean {
        val windowingSystem = world[WindowingSystem]
        if (windowingSystem.updateGeometry(Application.frameBufferSize)) {
            needsRerender = true
        }
        return needsRerender
    }

    fun quadToVertices(quad: WQuad, bmin: Vec2i, bmax: Vec2i) {
        val tc = if (quad.image != null) { textureBlock.getOrUpdate(quad.image!!) } else { textureBlock.blankTexture() }

        val t = Vec2f()
        for (q in 0 until 4) {
            val sr = quad.subRect
            if (sr != null) {
                t.x = tc.texPosition.x + sr.x * tc.texDimensions.x + sr.width * tc.texDimensions.x * UnitSquare2D[q].x
                t.y = tc.texPosition.y + sr.y * tc.texDimensions.y + sr.height * tc.texDimensions.y * UnitSquare2D[q].y
            } else {
                t.x = tc[q].x
                t.y = tc[q].y
            }

            vao.addV().apply {
                boundsMin = bmin
                boundsMax = bmax
                vertex = quad.position + Vec3i(quad.dimensions.x * UnitSquare2Di[q].x, quad.dimensions.y * UnitSquare2Di[q].y, 0)
                texCoord = t
                color = quad.color ?: White
            }
        }

        vao.addIQuad()
    }

    fun recursiveDraw(w: Widget) {
        if (! w.showing()) {
            return
        }

        val bmin = w.bounds.min()
        val bmax = w.bounds.max()

        for (quad in w.quads) {
            if (quad.beforeChildren) {
                quadToVertices(quad, bmin, bmax)
            }
        }

        for (c in w.children) {
            recursiveDraw(c)
        }

        for (quad in w.quads) {
            if (! quad.beforeChildren) {
                quadToVertices(quad, bmin, bmax)
            }
        }
    }

    override fun draw(world: World) {
        val windowingSystem = world[WindowingSystem]
        if (needsRerender) {
            vao.reset()
            recursiveDraw(windowingSystem.desktop)
            needsRerender = false
        }

        shader.bind()

        shader.setUniform("ProjectionMatrix", windowingSystem.projectionMatrix())

        textureBlock.bind()

        vao.sync()
        vao.draw()
    }

    override fun handleEvent(world: World, event: Event) {
        val windowingSystem = world[WindowingSystem]
        when (event) {
            is DisplayEvent -> windowingSystem.handleEvent(event)
        }
    }
}