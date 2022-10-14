package arx.core

import arx.display.core.Image
import arx.display.core.SimpleQuad
import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import java.text.AttributedString
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

val FontIdCounter = AtomicInteger(1)

class GlyphRenderer {
    val timer = Metrics.timer("GlyphRenderer.render")
    val buffer = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
    val g: Graphics2D = buffer.createGraphics().apply {
        background = Color(255,255,255,255)
    }

    fun render(shape: Shape): Image {
        val ctx = timer.time()
        g.transform = AffineTransform()
        g.composite = AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f)
        g.color = Color(255, 255, 255, 255)
        g.fillRect(0, 0, shape.bounds.width + 1, shape.bounds.height + 1)
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)
        g.transform = AffineTransform.getTranslateInstance(-shape.bounds.minX, -shape.bounds.minY)
        g.color = Color(255, 255, 255, 255)
        g.fill(shape)
        val img = Image.ofSize(shape.bounds.width, shape.bounds.height)
        val v = RGBA()
        val rast = buffer.data
        for (y in 0 until shape.bounds.height) {
            for (x in 0 until shape.bounds.width) {
                v.r = rast.getSample(x, y, 0).toUByte()
                v.g = rast.getSample(x, y, 1).toUByte()
                v.b = rast.getSample(x, y, 2).toUByte()
                v.a = rast.getSample(x, y, 3).toUByte()

                img[x, img.height - y - 1] = v
            }
        }
        ctx.stop()
        return img
    }

    fun fontMetrics(font: Font?): FontMetrics {
        return g.getFontMetrics(font)
    }
}

class ArxTypeface(val baseFont: Font) {
    val renderer = GlyphRenderer()
    val fontsBySize = mutableMapOf<Int, ArxFont>()
    var baseSize = 9

    fun withSize(size: Int) : ArxFont {
        val effSize = ceil(size.toFloat() / baseSize.toFloat()).toInt() * baseSize
        return fontsBySize.getOrPut(effSize) { ArxFont(baseFont.deriveFont(effSize.toFloat()), effSize, this) }
    }

    companion object {
        fun load(path: String) : ArxTypeface {
            return ArxTypeface(Font.createFont(Font.TRUETYPE_FONT, File(path)))
        }
    }
}

class ArxFont(val font: Font, val size: Int, val typeface: ArxTypeface) {
    val id = FontIdCounter.getAndIncrement()
    val glyphCache = mutableMapOf<Int, Image>()
    var fontMetrics = typeface.renderer.fontMetrics(font)

    fun withSize(size: Int) : ArxFont {
        return typeface.withSize(size)
    }
}



sealed interface RichTextSegment

data class SimpleTextSegment (val text: String) : RichTextSegment
data class StyledTextSegment (val text: String, val color: RGBA? = null, val font: ArxFont? = null) : RichTextSegment
data class ImageSegment (val image: Image, val color: RGBA? = null, val size: Int? = null) : RichTextSegment

data class RichText(
    val segments : MutableList<RichTextSegment> = mutableListOf(),
    var color: RGBA? = null,
    var font: ArxFont? = null
) {
    constructor (str: String, color: RGBA? = null, font: ArxFont? = null) : this() {
        if (str.isNotEmpty()) {
            if (color != null || font != null) {
                segments.add(StyledTextSegment(str, color, font))
            } else {
                segments.add(SimpleTextSegment(str))
            }
        }
    }
}

class TextLayout {
    val quads = mutableListOf<SimpleQuad>()

    /**
     * Bounds in absolute coordinates of the full occupied area of the text
     * not relative to the region.
     */
    var min = Vec2i(0,0)
    var max = Vec2i(0,0)
    var endCursorPos = Vec2i(0,0)

    val bounds : Recti
        get() { return Recti(min.x, min.y, max.x - min.x + 1, max.y - min.y + 1)}

    fun empty(): Boolean {
        return min == max
    }

    companion object {
        val frc = FontRenderContext(AffineTransform(), false, false)
        val defaultFont = Resources.typeface("arx/fonts/ChevyRayExpress.ttf").withSize(9)

        fun layout(text: RichText, position: Vec2i, region: Recti, defaultFont: ArxFont = TextLayout.defaultFont) : TextLayout {
            val ret = TextLayout()
            ret.min.x = position.x
            ret.min.y = position.y
            ret.max.x = position.x
            ret.max.y = position.y
            val cursor = Vec2i(position.x, position.y + defaultFont.fontMetrics.maxAscent)
            for (segment in text.segments) {
                layoutSegment(ret, text, segment, cursor, region, defaultFont)
            }
            ret.endCursorPos = cursor
            return ret
        }

        private fun layoutTextSegment(layout: TextLayout, rt: RichText, text: String, color: RGBA?, cursor: Vec2i, region: Recti, font: ArxFont) {
            if (region.width <= 0 || region.height <= 0) {
                return
            }

            val g = RecordingGlyphRenderGraphics(font, layout.quads)
            val attrStr = AttributedString(text)
            attrStr.addAttribute(TextAttribute.FONT, font.font)
            val iter = attrStr.iterator
            val lbm = LineBreakMeasurer(iter, frc)

            val quadsPreIndex = layout.quads.size
            while (lbm.position < iter.endIndex) {
                layout.min.x = min(layout.min.x, cursor.x)
                layout.max.y = max(layout.max.y, cursor.y + font.fontMetrics.descent)
                val tl = lbm.nextLayout((region.x + region.width - cursor.x).toFloat())

                tl.draw(g, cursor.x.toFloat(), cursor.y.toFloat())

                cursor.x = region.x
                if (lbm.position < iter.endIndex) {
                    cursor.y += font.fontMetrics.height
                }
            }

            for (qi in quadsPreIndex until g.quads.size) {
                g.quads[qi].color = color ?: rt.color
                layout.max.x = max(layout.max.x, g.quads[qi].position.x.toInt() + g.quads[qi].dimensions.x.toInt())

            }
        }

        private fun layoutSegment(layout : TextLayout, rt: RichText, segment: RichTextSegment, position: Vec2i, region: Recti, defaultFont: ArxFont) {
            return when (segment) {
                is SimpleTextSegment -> layoutTextSegment(layout, rt, segment.text, rt.color, position, region, rt.font ?: defaultFont)
                is StyledTextSegment -> layoutTextSegment(layout, rt, segment.text, segment.color ?: rt.color, position, region, segment.font ?: rt.font ?: defaultFont)
                is ImageSegment -> TODO("Image segments not yet implemented")
            }
        }
    }

    override fun toString(): String {
        val quadStr = quads.joinToString("\n\t\t") { q -> "pos: ${q.position}, dim: ${q.dimensions}" }
        return """TextLayout {
            |   bounds: $bounds
            |   rects: [
            |       $quadStr
        |       ]
            |}
        """.trimMargin()
    }


}


fun main() {
    System.setProperty("java.awt.headless", "true")

    val testText = "this is a jest, the quick brown fox jumped over the lazy dog. THE QUICK BROWN FOX JUMPED OVER THE LAZY DOG"

    val rt = RichText(testText)

    val layout = TextLayout.layout(rt, Vec2i(100,100), Recti(50,50,300, 300))
    println(layout)

//    Metrics.print()
}

class RecordingGlyphRenderGraphics(val font: ArxFont, val quads : MutableList<SimpleQuad> = mutableListOf()) : UnimplementedGraphics() {


    override fun drawGlyphVector(g: GlyphVector?, x: Float, y: Float) {
        if (g != null) {
            val cache = font.glyphCache

            for (i in 0 until g.numGlyphs) {
                val shape = g.getGlyphOutline(i, 0.0f, 0.0f)
                val code = g.getGlyphCode(i)

                var img = cache[code]
                if (img == null) {
                    img = if (shape.bounds.width == 0) {
                        Image.ofSize(0,0)
                    } else {
                        font.typeface.renderer.render(shape)
                    }

                    cache[code] = img
                }
                val bounds = shape.bounds
                if (bounds.width > 0.0f && bounds.height > 0.0f) {
                    quads.add(
                        SimpleQuad(
                            position = Vec2f(bounds.x.toFloat() + x, bounds.y.toFloat() + y),
                            dimensions = Vec2f(bounds.width.toFloat(), bounds.height.toFloat()),
                            color = RGBA(0,0,0,255),
                            image = img
                        )
                    )
                }
            }

        }
    }

    override fun getFont(): Font {
        return font.font
    }

    override fun getFontRenderContext(): FontRenderContext {
        return FontRenderContext(AffineTransform(), false, false)
    }

    override fun getFontMetrics(f: Font?): FontMetrics {
        if (f === font.font) {
            return font.fontMetrics
        } else {
            error("Recording glyph renderer must use the same font as it is constructed with")
        }
    }
}