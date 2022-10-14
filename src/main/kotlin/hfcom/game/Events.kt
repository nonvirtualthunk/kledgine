package hfcom.game

import arx.display.core.MouseButton
import arx.engine.*
import hfcom.display.MapCoord
import hfcom.display.MapCoord2D


data class CharacterMoved(val character: Entity, val from: MapCoord, val to: MapCoord) : GameEvent()

data class CharacterUsedAP(val character : Entity, val apConsumed : Double, val apRemaining : Double) : GameEvent() {
    val apBefore : Double get() { return apRemaining + apConsumed }
}
data class CharacterPlaced(val character: Entity, val at : MapCoord) : GameEvent()

data class CharacterPerformedAction(val character: Entity, val action: Action, val targets: List<EffectTarget>) : GameEvent()

data class CharacterAttacked(val character : Entity, val target : Entity) : GameEvent()

data class DamageTaken(val character : Entity, val hpLostBefore : Int, val hpLostAfter : Int) : GameEvent()

data class CharacterDied(val character : Entity) : GameEvent()


data class TMMouseReleaseEvent(val position : MapCoord2D, val button : MouseButton, val source : MouseReleaseEvent) : DisplayEvent(source)
data class TMMousePressEvent(val position : MapCoord2D, val button : MouseButton, val source : MousePressEvent) : DisplayEvent(source)
data class TMMouseMoveEvent(val position : MapCoord2D, val source : MouseMoveEvent) : DisplayEvent(source)
data class TMMouseDragEvent(val position : MapCoord2D, val source : MouseDragEvent) : DisplayEvent(source)