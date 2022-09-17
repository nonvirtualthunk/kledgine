package arx.application

import arx.core.*
import arx.display.windowing.*
import arx.display.windowing.components.TextDisplay
import arx.engine.Engine

fun main() {
    val windowingSystem = WindowingSystem().apply {
        registerStandardComponents()
        createWidget().apply {
            x = WidgetPosition.Fixed(100)
            y = WidgetPosition.Fixed(100)
            width = WidgetDimensions.Fixed(200)
            height = WidgetDimensions.Fixed(100)
            identifier = "Child A"
            background.color = bindable(RGBA(180u,180u,180u,255u))
        }

        createWidget().apply {
            x = WidgetPosition.Fixed(150)
            y = WidgetPosition.Fixed(300)
            width = WidgetDimensions.Intrinsic()
            height = WidgetDimensions.Intrinsic()
            identifier = "Child B"
            background.color = bindable(RGBA(200u, 200u, 200u, 255u))
            padding = Vec3i(4,4,0)
            attachData(TextDisplay(
                text = bindable(RichText("Test"))
            ))
        }

        desktop.onEventDo<WidgetMouseReleaseEvent> { e ->
            println("Widget mouse release : ${e.position} <- ${e.from.position}")
        }
    }

    val engine = Engine(
        mutableListOf(),
        mutableListOf(WindowingSystemComponent(windowingSystem))
    )

    Application()
        .run(engine)
}