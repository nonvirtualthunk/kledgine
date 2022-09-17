package arx.display.core


import arx.core.RGBA as vRGBA


typealias RGBA = vRGBA;

fun RGBA(r: Int, g: Int, b: Int, a: Int): RGBA {
    return RGBA(r.toUByte(), g.toUByte(), b.toUByte(), a.toUByte())
}


val White = RGBA(255,255,255,255)
val Black = RGBA(0,0,0,255)
val Clear = RGBA(255,255,255,0)