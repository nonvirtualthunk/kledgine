package arx.display.windowing.components

import arx.core.*
import arx.display.core.Image
import arx.core.White
import arx.display.windowing.*
import arx.engine.DataType
import arx.engine.DisplayData
import arx.engine.EntityData
import arx.engine.World
import com.typesafe.config.ConfigValue


internal val scaleToFitPattern = Regex("(?i)scale\\s?to\\s?fit")
internal val scaleFractionPattern = Regex("([\\d.]+)")
internal val scalePercentPattern = Regex("(\\d+)%")
internal val scaleToAxisPattern = Regex("(?i)scale\\s?to\\s?(width|height)\\(?(\\d+)px\\)?")
internal val exactPattern = Regex("(?i)exact")

interface ImageScale {
    fun transform(axis : Axis2D, v : Int) : Int

    data class Proportional(val proportion : Vec2f) : ImageScale {
        constructor (x : Float, y : Float) : this(Vec2f(x, y))
        constructor (x : Float) : this(Vec2f(x, x))

        override fun transform(axis: Axis2D, v: Int) : Int {
            return (proportion[axis] * v.toFloat()).toInt()
        }
    }

    data class Absolute(val dimensions : Vec2i) : ImageScale {
        constructor (width : Int, height : Int) : this(Vec2i(width, height))
        constructor (size : Int) : this(Vec2i(size, size))

        override fun transform(axis: Axis2D, v: Int): Int {
            return dimensions[axis]
        }
    }

    object Exact : ImageScale {
        override fun transform(axis: Axis2D, v: Int): Int {
            return v
        }
    }
    
    companion object : FromConfigCreator<ImageScale> {
        override fun createFromConfig(cv: ConfigValue?): ImageScale? {
            if (cv != null && cv.isStr()) {
                val str = cv.asStr() ?: ""
                scaleFractionPattern.match(str)?.let { (scale) ->
                    return Proportional(scale.toFloat())
                }
                scalePercentPattern.match(str)?.let { (pcnt) ->
                    return Proportional(pcnt.toFloat() / 100.0f)
                }
                exactPattern.match(str)?.let {
                    return Exact
                }

            }
            Noto.warn("Invalid config for image scale: $cv")
            return null
        }
    }
}

data class ImageWidget (
    var image : Bindable<Image?> = ValueBindable.Null(),
    var color : Bindable<RGBA?> = ValueBindable.Null(),
    var scale : ImageScale = ImageScale.Exact
) : DisplayData {
    companion object : DataType<ImageWidget>( ImageWidget() ), FromConfigCreator<ImageWidget> {
        override fun createFromConfig(cv: ConfigValue?): ImageWidget? {
            return if (cv["image"] != null) {
                ImageWidget(
                    image = cv["image"]?.let { bindableImage(it) } ?: ValueBindable.Null(),
                    color = cv["color"]?.let { bindableRGBA(it) } ?: ValueBindable.Null(),
                    scale = cv["scale"]?.let { ImageScale(it) } ?: ImageScale.Exact
                )
            } else {
                null
            }
        }
    }
    override fun dataType() : DataType<*> { return ImageWidget }

    fun copy() : ImageWidget {
        return ImageWidget(image = image.copyBindable(), color = color.copyBindable(), scale = scale)
    }
}

operator fun ImageWidget?.unaryPlus() : ImageWidget {
    return this?: ImageWidget.defaultInstance
}


object ImageDisplayWindowingComponent : WindowingComponent {
    override fun dataTypes() : List<DataType<EntityData>> {
        return listOf(ImageWidget)
    }

    override fun intrinsicSize(w: Widget, axis: Axis2D, minSize: Vec2i, maxSize: Vec2i): Int? {
        return w[ImageWidget]?.let { iw ->
            iw.image()?.let { img ->
                iw.scale.transform(axis, img.dimensions[axis])
            }
        }
    }

    override fun render(ws: WindowingSystem, w: Widget, bounds: Recti, quadsOut: MutableList<WQuad>) {
        w[ImageWidget]?.let { iw ->
            iw.image()?.let { img ->
                quadsOut.add(WQuad(
                    position = Vec3i(w.resClientX, w.resClientY, 0),
                    dimensions = Vec2i(w.resClientWidth, w.resClientHeight),
                    image = img,
                    color = iw.color(),
                    beforeChildren = true,
                    subRect = Rectf(0.0f,1.0f,1.0f,-1.0f)
                ))
            }
        }
    }

    override fun updateBindings(ws: WindowingSystem, w: Widget, ctx: BindingContext) {
        w[ImageWidget]?.let { iw ->
            if (iw.image.update(ctx)) {
                w.markForUpdate(RecalculationFlag.Contents)
                if (w.dimensions(Axis2D.X).isIntrinsic()) {
                    w.markForUpdate(RecalculationFlag.DimensionsX)
                }
                if (w.dimensions(Axis2D.Y).isIntrinsic()) {
                    w.markForUpdate(RecalculationFlag.DimensionsY)
                }
            }
        }
    }
}