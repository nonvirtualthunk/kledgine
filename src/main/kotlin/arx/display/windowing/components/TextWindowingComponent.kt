package arx.display.windowing.components

import arx.core.*
import arx.display.windowing.*
import arx.engine.DataType
import arx.engine.DisplayData
import arx.engine.EntityData
import arx.engine.World
import com.typesafe.config.ConfigValue
import kotlin.math.max
import kotlin.math.min


data class TextDisplay(
    var text: Bindable<RichText> = bindable(RichText()),
    var multiLine: Boolean = false
) : DisplayData {
    companion object : DataType<TextDisplay>(TextDisplay()), FromConfigCreator<TextDisplay> {
        override fun createFromConfig(cv: ConfigValue?): TextDisplay? {
            return if (cv["text"] != null) {
                TextDisplay(
                    text = cv["text"].ifLet { bindableRichText(it) }.orElse { bindable(RichText()) },
                    multiLine = cv["mutliLine"]?.asBool() ?: false
                )
            } else {
                null
            }
        }
    }

    override fun dataType(): DataType<*> {
        return TextDisplay
    }

    fun copy() : TextDisplay {
        return TextDisplay(text = text.copyBindable(), multiLine = multiLine)
    }
}


object TextWindowingComponent : WindowingComponent {

    val layoutCache = LRULoadingCache<Pair<RichText, Recti>, TextLayout>(500, 0.5f) {
        TextLayout.layout(it.first, it.second.position, it.second)
    }

    override fun dataTypes() : List<DataType<EntityData>> {
        return listOf(TextDisplay)
    }

    override fun intrinsicSize(w: Widget, axis: Axis2D, minSize: Vec2i, maxSize: Vec2i): Int? {
        val td = w[TextDisplay] ?: return null

        val region = Recti(0, 0, maxSize.x, maxSize.y)
        val layout = layoutCache.getOrPut(td.text() to region)

        return layout.max[axis] - layout.min[axis]
    }

    override fun render(ws: WindowingSystem, w: Widget, bounds: Recti, quadsOut: MutableList<WQuad>) {
        val td = w[TextDisplay] ?: return

        val region = Recti(w.resClientX, w.resClientY, w.resClientWidth + 1, w.resClientHeight + 1)
        val layout = layoutCache.getOrPut(td.text() to region)

        for (squad in layout.quads) {
            quadsOut.add(WQuad(
                position = Vec3i(squad.position.x.toInt(), squad.position.y.toInt(), 0),
                dimensions = Vec2i(squad.dimensions.x.toInt(), squad.dimensions.y.toInt()),
                image = squad.image,
                color = squad.color ?: Black,
                beforeChildren = true,
                subRect = Rectf(0.0f, 1.0f, 1.0f, -1.0f)
            ))
        }
    }

    override fun updateBindings(ws: WindowingSystem, w: Widget, ctx: BindingContext) {
        val td = w[TextDisplay] ?: return

        if (td.text.update(ctx)) {
            w.markForUpdate(RecalculationFlag.Contents)
            if (w.dimensions(Axis2D.X).isIntrinsic() ) {
                w.markForUpdate(RecalculationFlag.DimensionsX)
            }
            if( w.dimensions(Axis2D.Y).isIntrinsic()) {
                w.markForUpdate(RecalculationFlag.DimensionsY)
            }
        }
    }
}