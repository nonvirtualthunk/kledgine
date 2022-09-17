package arx.display.windowing.components

import arx.core.*
import arx.display.windowing.*
import arx.engine.DataType
import arx.engine.DisplayData
import arx.engine.EntityData
import arx.engine.World
import kotlin.math.max
import kotlin.math.min


data class TextDisplay(
    var text: Bindable<RichText> = bindable(RichText()),
    var multiLine: Boolean = false
) : DisplayData {
    companion object : DataType<TextDisplay>(TextDisplay())

    override fun dataType(): DataType<*> {
        return TextDisplay
    }
}

operator fun TextDisplay?.unaryPlus(): TextDisplay {
    return this ?: TextDisplay.defaultInstance
}


object TextWindowingComponent : WindowingComponent {

    val layoutCache = LRULoadingCache<Pair<RichText, Recti>, TextLayout>(500, 0.5f) {
        TextLayout.layout(it.first, it.second.position, it.second)
    }

    override fun registerDataTypes(world: World) {
        world.register(TextDisplay)
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
                color = squad.color,
                beforeChildren = true,
                subRect = Rectf(0.0f, 1.0f, 1.0f, -1.0f)
            ))
        }
    }
}