package arx.display.windowing.components

import arx.core.*
import arx.display.windowing.*
import arx.engine.DataType
import arx.engine.DisplayData
import arx.engine.DisplayEvent
import arx.engine.EntityData
import com.typesafe.config.ConfigValue

data class FocusSettings (
    val acceptsFocus : Boolean = false,
    val focusedOverlay : NineWayImage? = null
) : DisplayData {
    companion object : DataType<FocusSettings>( FocusSettings() ), FromConfigCreator<FocusSettings> {
        override fun createFromConfig(cv: ConfigValue?): FocusSettings? {
            if (cv == null || cv["acceptsFocus"] == null) { return null }
            return FocusSettings(
                acceptsFocus = cv["acceptsFocus"].asBool() ?: false,
                focusedOverlay = cv["focusedOverlay"]?.let { NineWayImage(it) }
            )
        }
    }
    override fun dataType() : DataType<*> { return FocusSettings }
}

operator fun FocusSettings?.unaryPlus() : FocusSettings {
    return this?: FocusSettings.defaultInstance
}


data class FocusChangedEvent(override val widgets: MutableList<Widget>, val hasFocus: Boolean, val src : DisplayEvent) : WidgetEvent(src)

object FocusWindowingComponent : WindowingComponent {

    override fun dataTypes(): List<DataType<EntityData>> {
        return listOf(FocusSettings)
    }

    override fun render(ws: WindowingSystem, w: Widget, bounds: Recti, quadsOut: MutableList<WQuad>) {
        val fs = w[FocusSettings] ?: return
        fs.focusedOverlay?.let { overlay ->
            if (ws.focusedWidget == w) {
                BackgroundComponent.renderNineWay(w, overlay, false, quadsOut)
            }
        }
    }

    override fun handleEvent(w: Widget, event: DisplayEvent): Boolean {
        when(event) {
            is WidgetMouseReleaseEvent -> {
                val focusSettings = w[FocusSettings] ?: return false
                if (focusSettings.acceptsFocus) {
                    if (w.windowingSystem.focusedWidget != w) {
                        Noto.info("Focus given to $w")
                        w.windowingSystem.focusedWidget?.let { fw ->
                            w.windowingSystem.fireEvent(FocusChangedEvent(mutableListOf(fw), false, event))
                        }
                        w.windowingSystem.focusedWidget = w
                        w.windowingSystem.fireEvent(FocusChangedEvent(mutableListOf(w), true, event))
                    }
                }
            }
        }
        return false
    }
}