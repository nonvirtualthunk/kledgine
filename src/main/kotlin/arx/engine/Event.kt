package arx.engine

import arx.core.Vec2f
import arx.core.Vec2i
import arx.display.core.Key
import arx.display.core.KeyModifiers
import arx.display.core.MouseButton

open class Event {
    var consumed: Boolean = false

    fun consume() { consumed = true }
}


open class GameEvent : Event()

open class DisplayEvent(var parentEvent : DisplayEvent? = null) : Event() {
    fun withParentEvent(p : DisplayEvent) : DisplayEvent {
        parentEvent = p
        return this
    }
}


sealed class MouseEvent : DisplayEvent() {
    abstract val position: Vec2f
}
sealed class KeyEvent : DisplayEvent() {
    abstract val key: Key
}

data class KeyReleaseEvent(override val key: Key, val mods: KeyModifiers) : KeyEvent()
data class KeyPressEvent(override val key: Key, val mods: KeyModifiers) : KeyEvent()
data class KeyRepeatEvent(override val key: Key, val mods: KeyModifiers) : KeyEvent()
data class CharInputEvent(val char : Char) : DisplayEvent()
data class MousePressEvent(override val position: Vec2f, val button: MouseButton, val mods: KeyModifiers) : MouseEvent()
data class MouseReleaseEvent(override val position: Vec2f, val button: MouseButton, val mods: KeyModifiers) : MouseEvent()
data class MouseMoveEvent(override val position: Vec2f, val delta: Vec2f, val mods: KeyModifiers) : MouseEvent()
data class MouseDragEvent(override val position: Vec2f, val delta: Vec2f, val button: MouseButton, val mods: KeyModifiers) : MouseEvent()


data class WindowSizeChangedEvent(val dimensions: Vec2i) : DisplayEvent()
data class FrameBufferSizeChangedEvent(val dimensions: Vec2i): DisplayEvent()