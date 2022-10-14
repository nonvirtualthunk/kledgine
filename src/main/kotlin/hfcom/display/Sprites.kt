package hfcom.display

import arx.core.*
import arx.display.core.Image
import arx.display.core.ImageRef

object Sprites {

    private val imageOutlines : MutableMap<Pair<Image, List<RGBA>>, Image> = mutableMapOf()



    fun imageOutline(baseImgRef : ImageRef, palette : List<RGBA>) : Image {
        val baseImg = baseImgRef.toImage()
        return imageOutlines.getOrPut(baseImg to palette) {
            val ret = Image.ofSize(baseImg.width, baseImg.height)
            var prev : Image? = null

            for ((i, c) in palette.withIndex()) {
                ret.transformPixels { x, y, v ->
                    if (v.a.toUInt() == 0u && baseImg.sample(x,y,3) <= 0u) {
                        var foundAdj = false
                        for (q in 0 until 4) {
                            val ax = x + Cardinals2D[q].x
                            val ay = y + Cardinals2D[q].y
                            if (ax >= 0 && ay >= 0 && ax < baseImg.width && ay < baseImg.height) {
                                if (baseImg.sample(ax, ay, 3) > 0u) {
                                    foundAdj = true
                                    break
                                }
                                if (i > 0 && prev!!.sample(ax, ay, 3) > 0u) {
                                    foundAdj = true
                                    break
                                }
                            }
                        }
                        if (foundAdj) {
                            v(c.r, c.g, c.b, c.a)
                        }
                    }
                }
                if (i < palette.size - 1) {
                    prev.ifLet {
                        it.copyFrom(ret, Vec2i(0,0))
                    }.orElse {
                        prev = Image.copy(ret)
                    }
                }
            }

            prev.ifLet { it.destroy() }

            ret
        }
    }
}


fun main () {
    Sprites.imageOutline(Resources.image("hfcom/display/characters/cultist_4.png"), listOf(RGBA(123u, 14u, 14u, 255u), RGBA(170u, 29u, 29u, 255u), RGBA(193u, 62u, 62u, 255u)))
        .writeToFile("/tmp/outlined.png")
}