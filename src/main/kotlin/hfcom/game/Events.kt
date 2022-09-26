package hfcom.game

import arx.display.core.MouseButton
import arx.engine.*
import hfcom.display.MapCoord
import hfcom.display.MapCoord2D


data class CharacterMoved(val character: Entity, val from: MapCoord, val to: MapCoord) : GameEvent()





data class TMMouseReleaseEvent(val position : MapCoord2D, val button : MouseButton, val source : MouseReleaseEvent) : DisplayEvent(source)
data class TMMousePressEvent(val position : MapCoord2D, val button : MouseButton, val source : MousePressEvent) : DisplayEvent(source)
data class TMMouseMoveEvent(val position : MapCoord2D, val source : MouseMoveEvent) : DisplayEvent(source)
data class TMMouseDragEvent(val position : MapCoord2D, val source : MouseDragEvent) : DisplayEvent(source)