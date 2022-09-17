package arx.display.core

import arx.core.Vec2f

data class SimpleQuad(var position: Vec2f, var dimensions: Vec2f, var image: Image, var color: RGBA? = RGBA(255,255,255,255))