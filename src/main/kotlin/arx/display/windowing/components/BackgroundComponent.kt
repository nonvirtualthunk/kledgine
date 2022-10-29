package arx.display.windowing.components

import arx.core.*
import arx.display.core.Image
import arx.display.windowing.*

private data class ImageMetrics(
    val borderWidth: Int,
    val centerColor: RGBA,
    val outerOffset: Int
)

object BackgroundComponent : WindowingComponent {

    private val imageMetrics = mutableMapOf<Image, ImageMetrics>()

    private fun computeImageMetrics(img: Image): ImageMetrics {
        var outerOffset = 0
        while (outerOffset < img.width && img.sample(outerOffset, img.height / 2, 3).toUInt() == 0u) {
            outerOffset += 1
        }
        // if it's entirely transparent treat it as having no offset
        if (outerOffset == img.width) {
            outerOffset = 0
        }
        var borderWidth = outerOffset
        while (borderWidth < img.width && img.sample(borderWidth, img.height / 2, 3).toUInt() > 0u) {
            borderWidth += 1
        }
        // if it's entirely transparent, treat it as having no border either
        if (borderWidth == img.width) {
            borderWidth = 0
        }

        val centerColor = img[img.width / 2, img.height / 2]
        return ImageMetrics(borderWidth, centerColor, outerOffset)
    }

    private fun imageMetricsFor(img: Image) : ImageMetrics {
        return imageMetrics.getOrPut(img) { computeImageMetrics(img) }
    }

    override fun clientOffsetContribution(w: Widget, axis: Axis2D): Int {
        return if (w.background.draw()) {
            val img = w.background.image().toImage()
            val metrics = imageMetricsFor(img)
            metrics.borderWidth
        } else {
            0
        }
    }

    fun cornerQuad(w: Widget, img: Image, id: Vec2i, color: RGBA?, xd: Int, yd: Int, beforeChildren: Boolean): WQuad {
        val wp = w.resolvedPosition
        val wd = w.resolvedDimensions

        return WQuad(
            position = wp + Vec3i((wd.x - id.x) * xd, (wd.y - id.y) * yd, 0),
            dimensions = id,
            image = img,
            color = color,
            beforeChildren = beforeChildren,
            subRect = Rectf(xd.toFloat() * 0.6666f, yd.toFloat() * 0.6666f, 0.3333f, 0.3333f)
        )
    }

    fun edgeQuad(w: Widget, img: Image, id: Vec2i, color: RGBA?, xd: Int, yd: Int, beforeChildren: Boolean): WQuad {
        val wp = w.resolvedPosition
        val wd = w.resolvedDimensions

        return if (xd == -1 && yd == 0) {
            WQuad(
                position = wp + Vec3i(0, id.y, 0),
                dimensions = Vec2i(id.x, wd.y - id.y * 2),
                image = img,
                color = color,
                beforeChildren = beforeChildren,
                subRect = Rectf(0.0f, 0.3333f, 0.3333f, 0.3333f)
            )
        } else if (xd == 1 && yd == 0) {
            WQuad(
                position = wp + Vec3i(wd.x - id.x, id.y, 0),
                dimensions = Vec2i(id.x, wd.y - (id.y) * 2),
                image = img,
                color = color,
                beforeChildren = beforeChildren,
                subRect = Rectf(0.6666f, 0.3333f, 0.3333f, 0.3333f)
            )
        } else if (xd == 0 && yd == -1) {
            WQuad(
                position = wp + Vec3i(id.x, 0, 0),
                dimensions = Vec2i(wd.x - (id.x) * 2, id.y),
                image = img,
                color = color,
                beforeChildren = beforeChildren,
                subRect = Rectf(0.3333f, 0.0f, 0.3333f, 0.3333f)
            )
        } else {
            WQuad(
                position = wp + Vec3i(id.x, wd.y - id.y, 0),
                dimensions = Vec2i(wd.x - (id.x) * 2, id.y),
                image = img,
                color = color,
                beforeChildren = beforeChildren,
                subRect = Rectf(0.3333f, 0.6666f, 0.3333f, 0.3333f)
            )
        }

    }

    private fun centerQuad(w: Widget, img: Image, scale: Int, color: RGBA?, beforeChildren: Boolean): WQuad {
        val wp = w.resolvedPosition
        val wd = w.resolvedDimensions
        val imm = imageMetricsFor(img)

        return WQuad(
            position = wp + Vec3i(imm.borderWidth * scale, imm.borderWidth * scale),
            dimensions = Vec2i(wd.x - imm.borderWidth * scale * 2, wd.y - imm.borderWidth * scale * 2),
            image = img,
            color = color,
            beforeChildren = beforeChildren,
            subRect = Rectf(0.5f, 0.5f, 0.0f, 0.0f)
        )
    }

    fun renderNineWay(w: Widget, nw: NineWayImage, beforeChildren: Boolean, out: MutableList<WQuad>) {
        if (nw.draw()) {
            val img = nw.image().toImage()
            val id = ((img.dimensions + 2) / 3) * nw.scale

            if (nw.drawCenter()) {
                out.add(centerQuad(w, img, nw.scale, nw.centerColor() ?: nw.color(), beforeChildren))
            }

            for (x in 0..1) {
                for (y in 0..1) {
                    out.add(cornerQuad(w, img, id, nw.edgeColor() ?: nw.color(), x, y, beforeChildren))
                }
            }
            if (nw.drawEdges()) {
                val c = nw.edgeColor() ?: nw.color()
                out.add(edgeQuad(w, img, id, c, -1, 0, beforeChildren))
                out.add(edgeQuad(w, img, id, c, 1, 0, beforeChildren))
                out.add(edgeQuad(w, img, id, c, 0, -1, beforeChildren))
                out.add(edgeQuad(w, img, id, c, 0, 1, beforeChildren))
            }
        }
    }

    override fun render(ws: WindowingSystem, w: Widget, bounds: Recti, quadsOut: MutableList<WQuad>) {
        renderNineWay(w, w.background, true, quadsOut)
        w.overlay?.let { renderNineWay(w, it, false, quadsOut) }
    }

    override fun updateBindings(ws: WindowingSystem, w : Widget, ctx: BindingContext) {
        val preClientOffsetsX = clientOffsetContribution(w, Axis2D.X)
        val preClientOffsetsY = clientOffsetContribution(w, Axis2D.Y)
        if (Bindable.updateBindableFields(w.background, ctx)) {
            w.markForUpdate(RecalculationFlag.Contents)
            if (preClientOffsetsX != clientOffsetContribution(w, Axis2D.X)) {
                w.markForUpdate(RecalculationFlag.DimensionsX)
                w.markForUpdate(RecalculationFlag.PositionX)
            }
            if (preClientOffsetsY != clientOffsetContribution(w, Axis2D.Y)) {
                w.markForUpdate(RecalculationFlag.DimensionsY)
                w.markForUpdate(RecalculationFlag.PositionY)
            }
        }
        w.overlay?.let {
            if (Bindable.updateBindableFields(it, ctx)) {
                w.markForUpdate(RecalculationFlag.Contents)
            }
        }
        if (w.showing.update(ctx)) {
            w.markForFullUpdate()
        }
    }
}