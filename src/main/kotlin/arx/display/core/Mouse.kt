package arx.display.core

import org.lwjgl.glfw.GLFW.*

enum class MouseButton(val code: Int) {
    Left(GLFW_MOUSE_BUTTON_LEFT),
    Right(GLFW_MOUSE_BUTTON_RIGHT),
    Middle(GLFW_MOUSE_BUTTON_MIDDLE);

    companion object {
        fun fromGLFW(v : Int) : MouseButton {
            return when (v) {
                GLFW_MOUSE_BUTTON_LEFT -> Left
                GLFW_MOUSE_BUTTON_RIGHT -> Right
                GLFW_MOUSE_BUTTON_MIDDLE -> Middle
                else -> Middle
            }
        }
    }

}