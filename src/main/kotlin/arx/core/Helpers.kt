package arx.core

import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ImageObserver
import java.awt.image.RenderedImage
import java.awt.image.renderable.RenderableImage
import java.text.AttributedCharacterIterator

open class UnimplementedGraphics : Graphics2D() {
    override fun drawGlyphVector(g: GlyphVector?, x: Float, y: Float) {
        TODO("Not implemented")
    }

    override fun create(): Graphics {
        TODO("Not implemented")
    }

    override fun translate(x: Int, y: Int) {
        TODO("Not implemented")
    }

    override fun translate(tx: Double, ty: Double) {
        TODO("Not implemented")
    }

    override fun getColor(): Color {
        TODO("Not implemented")
    }

    override fun setColor(c: Color?) {
        TODO("Not implemented")
    }

    override fun setPaintMode() {
        TODO("Not implemented")
    }

    override fun setXORMode(c1: Color?) {
        TODO("Not implemented")
    }

    override fun getFont(): Font {
        TODO("Not implemented")
    }

    override fun setFont(font: Font?) {
        TODO("Not implemented")
    }

    override fun getFontMetrics(f: Font?): FontMetrics {
        TODO("Not implemented")
    }

    override fun getClipBounds(): Rectangle {
        TODO("Not implemented")
    }

    override fun clipRect(x: Int, y: Int, width: Int, height: Int) {
        TODO("Not implemented")
    }

    override fun setClip(x: Int, y: Int, width: Int, height: Int) {
        TODO("Not implemented")
    }

    override fun setClip(clip: Shape?) {
        TODO("Not implemented")
    }

    override fun getClip(): Shape {
        TODO("Not implemented")
    }

    override fun copyArea(x: Int, y: Int, width: Int, height: Int, dx: Int, dy: Int) {
        TODO("Not implemented")
    }

    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        TODO("Not implemented")
    }

    override fun fillRect(x: Int, y: Int, width: Int, height: Int) {
        TODO("Not implemented")
    }

    override fun clearRect(x: Int, y: Int, width: Int, height: Int) {
        TODO("Not implemented")
    }

    override fun drawRoundRect(x: Int, y: Int, width: Int, height: Int, arcWidth: Int, arcHeight: Int) {
        TODO("Not implemented")
    }

    override fun fillRoundRect(x: Int, y: Int, width: Int, height: Int, arcWidth: Int, arcHeight: Int) {
        TODO("Not implemented")
    }

    override fun drawOval(x: Int, y: Int, width: Int, height: Int) {
        TODO("Not implemented")
    }

    override fun fillOval(x: Int, y: Int, width: Int, height: Int) {
        TODO("Not implemented")
    }

    override fun drawArc(x: Int, y: Int, width: Int, height: Int, startAngle: Int, arcAngle: Int) {
        TODO("Not implemented")
    }

    override fun fillArc(x: Int, y: Int, width: Int, height: Int, startAngle: Int, arcAngle: Int) {
        TODO("Not implemented")
    }

    override fun drawPolyline(xPoints: IntArray?, yPoints: IntArray?, nPoints: Int) {
        TODO("Not implemented")
    }

    override fun drawPolygon(xPoints: IntArray?, yPoints: IntArray?, nPoints: Int) {
        TODO("Not implemented")
    }

    override fun fillPolygon(xPoints: IntArray?, yPoints: IntArray?, nPoints: Int) {
        TODO("Not implemented")
    }

    override fun drawString(str: String, x: Int, y: Int) {
        TODO("Not implemented")
    }

    override fun drawString(str: String?, x: Float, y: Float) {
        TODO("Not implemented")
    }

    override fun drawString(iterator: AttributedCharacterIterator?, x: Int, y: Int) {
        TODO("Not implemented")
    }

    override fun drawString(iterator: AttributedCharacterIterator?, x: Float, y: Float) {
        TODO("Not implemented")
    }

    override fun drawImage(img: Image?, xform: AffineTransform?, obs: ImageObserver?): Boolean {
        TODO("Not implemented")
    }

    override fun drawImage(img: BufferedImage?, op: BufferedImageOp?, x: Int, y: Int) {
        TODO("Not implemented")
    }

    override fun drawImage(img: Image?, x: Int, y: Int, observer: ImageObserver?): Boolean {
        TODO("Not implemented")
    }

    override fun drawImage(img: Image?, x: Int, y: Int, width: Int, height: Int, observer: ImageObserver?): Boolean {
        TODO("Not implemented")
    }

    override fun drawImage(img: Image?, x: Int, y: Int, bgcolor: Color?, observer: ImageObserver?): Boolean {
        TODO("Not implemented")
    }

    override fun drawImage(img: Image?, x: Int, y: Int, width: Int, height: Int, bgcolor: Color?, observer: ImageObserver?): Boolean {
        TODO("Not implemented")
    }

    override fun drawImage(img: Image?, dx1: Int, dy1: Int, dx2: Int, dy2: Int, sx1: Int, sy1: Int, sx2: Int, sy2: Int, observer: ImageObserver?): Boolean {
        TODO("Not implemented")
    }

    override fun drawImage(img: Image?, dx1: Int, dy1: Int, dx2: Int, dy2: Int, sx1: Int, sy1: Int, sx2: Int, sy2: Int, bgcolor: Color?, observer: ImageObserver?): Boolean {
        TODO("Not implemented")
    }

    override fun dispose() {
        TODO("Not implemented")
    }

    override fun draw(s: Shape?) {
        TODO("Not implemented")
    }

    override fun drawRenderedImage(img: RenderedImage?, xform: AffineTransform?) {
        TODO("Not implemented")
    }

    override fun drawRenderableImage(img: RenderableImage?, xform: AffineTransform?) {
        TODO("Not implemented")
    }

    override fun fill(s: Shape?) {
        TODO("Not implemented")
    }

    override fun hit(rect: Rectangle?, s: Shape?, onStroke: Boolean): Boolean {
        TODO("Not implemented")
    }

    override fun getDeviceConfiguration(): GraphicsConfiguration {
        TODO("Not implemented")
    }

    override fun setComposite(comp: Composite?) {
        TODO("Not implemented")
    }

    override fun setPaint(paint: Paint?) {
        TODO("Not implemented")
    }

    override fun setStroke(s: Stroke?) {
        TODO("Not implemented")
    }

    override fun setRenderingHint(hintKey: RenderingHints.Key?, hintValue: Any?) {
        TODO("Not implemented")
    }

    override fun getRenderingHint(hintKey: RenderingHints.Key?): Any {
        TODO("Not implemented")
    }

    override fun setRenderingHints(hints: MutableMap<*, *>?) {
        TODO("Not implemented")
    }

    override fun addRenderingHints(hints: MutableMap<*, *>?) {
        TODO("Not implemented")
    }

    override fun getRenderingHints(): RenderingHints {
        TODO("Not implemented")
    }

    override fun rotate(theta: Double) {
        TODO("Not implemented")
    }

    override fun rotate(theta: Double, x: Double, y: Double) {
        TODO("Not implemented")
    }

    override fun scale(sx: Double, sy: Double) {
        TODO("Not implemented")
    }

    override fun shear(shx: Double, shy: Double) {
        TODO("Not implemented")
    }

    override fun transform(Tx: AffineTransform?) {
        TODO("Not implemented")
    }

    override fun setTransform(Tx: AffineTransform?) {
        TODO("Not implemented")
    }

    override fun getTransform(): AffineTransform {
        TODO("Not implemented")
    }

    override fun getPaint(): Paint {
        TODO("Not implemented")
    }

    override fun getComposite(): Composite {
        TODO("Not implemented")
    }

    override fun setBackground(color: Color?) {
        TODO("Not implemented")
    }

    override fun getBackground(): Color {
        TODO("Not implemented")
    }

    override fun getStroke(): Stroke {
        TODO("Not implemented")
    }

    override fun clip(s: Shape?) {
        TODO("Not implemented")
    }

    override fun getFontRenderContext(): FontRenderContext {
        TODO("Not implemented")
    }
}