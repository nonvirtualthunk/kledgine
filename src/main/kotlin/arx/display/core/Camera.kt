package arx.display.core

import arx.application.Application
import arx.core.Vec2f
import arx.core.Vec2i
import arx.core.Vec3f
import arx.engine.DisplayEvent
import arx.engine.KeyPressEvent
import arx.engine.KeyReleaseEvent
import dev.romainguy.kotlin.math.*
import org.lwjgl.glfw.GLFW
import kotlin.math.max

interface Camera {

    fun projectionMatrix(): Mat4

    fun modelviewMatrix(): Mat4

    fun handleEvent(event: DisplayEvent): Boolean

    fun update() : Boolean

    fun unproject(p : Vec2f) : Vec2f {
        val screenSpace = Float4((p.x / Application.windowSize.x) * 2.0f - 1.0f, ((Application.windowSize.y - p.y - 1) / Application.windowSize.y) * 2.0f - 1.0f, 0.0f, 1.0f)
        val res = inverse(projectionMatrix() * modelviewMatrix()).times(screenSpace)
        return Vec2f(res.x, res.y)
    }
}


class PixelCamera(var origin : Vec3f = Vec3f(0.0f, 0.0f, 0.0f), var scale : Float = 1.0f) : Camera {

    var frameBufferSize: () -> Vec2i = { Application.frameBufferSize }

    private var lastScale = 0.0f
    private var lastUpdateTime = 0.0

    var scaleIncrement = 1.0f

    var speed = Vec3f(16.0f, 16.0f, 16.0f)
    var delta = Vec3f(0.0f, 0.0f, 0.0f)

    override fun projectionMatrix(): Mat4 {
        val s = frameBufferSize()
        return translation(Float3(1f, 1f, 0.0f)) * ortho(0.0f, s.x.toFloat(), 0.0f, s.y.toFloat(), 0.0f, 100.0f) * scale(Float3((scale * scaleIncrement)))
    }

    override fun modelviewMatrix(): Mat4 {
        return translation(Float3(origin.x, origin.y, origin.z))
    }

    override fun handleEvent(event: DisplayEvent): Boolean {
        when (event) {
            is KeyPressEvent -> when(event.key) {
                Key.W -> delta.y = -1.0f
                Key.S -> delta.y = 1.0f
                Key.A -> delta.x = 1.0f
                Key.D -> delta.x = -1.0f
                Key.Z -> scale += 1.0f
                Key.X -> scale = max(scale - 1.0f, 1.0f)
                else -> return false
            }
            is KeyReleaseEvent -> when(event.key) {
                Key.W -> if (delta.y == -1.0f) { delta.y = 0.0f }
                Key.S -> if (delta.y == 1.0f) { delta.y = 0.0f }
                Key.A -> if (delta.x == 1.0f) { delta.x = 0.0f }
                Key.D -> if (delta.x == -1.0f) { delta.x = 0.0f }
                else -> return false
            }
            else -> return false
        }
        return true
    }

    override fun update() : Boolean {
        val newTime = GLFW.glfwGetTime()
        if (lastUpdateTime == 0.0) {
            lastUpdateTime = newTime
        }
        
        val deltaTime = newTime - lastUpdateTime
        lastUpdateTime = newTime
        origin.x = (origin.x.toDouble() + delta.x.toDouble() * speed.x.toDouble() * deltaTime).toFloat()
        origin.y = (origin.y.toDouble() + delta.y.toDouble() * speed.y.toDouble() * deltaTime).toFloat()
        origin.z = (origin.z.toDouble() + delta.z.toDouble() * speed.z.toDouble() * deltaTime).toFloat()

        val scaleChanged = lastScale != scale
        lastScale = scale
        return delta.x != 0.0f || delta.y != 0.0f || scaleChanged
    }
}