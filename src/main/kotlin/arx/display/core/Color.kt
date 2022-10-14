package arx.display.core


import arx.core.RGBA
import arx.core.clamp
import kotlin.math.roundToInt



fun mix(a: RGBA, b : RGBA, f : Float) : RGBA {
    val invF = 1.0f - f
    val newR = (a.r.toFloat() * f + b.r.toFloat() * invF).roundToInt().clamp(0, 255).toUByte()
    val newG = (a.g.toFloat() * f + b.g.toFloat() * invF).roundToInt().clamp(0, 255).toUByte()
    val newB = (a.b.toFloat() * f + b.b.toFloat() * invF).roundToInt().clamp(0, 255).toUByte()
    val newA = (a.a.toFloat() * f + b.a.toFloat() * invF).roundToInt().clamp(0, 255).toUByte()

    return RGBA(newR, newG, newB, newA)
}
