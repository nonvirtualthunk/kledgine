package arx.application

import arx.core.RGBA
import arx.core.RichText
import arx.core.Vec3i
import arx.core.bindable
import arx.display.windowing.*
import arx.display.windowing.components.DropdownSelectionChanged
import arx.display.windowing.components.TextDisplay
import arx.engine.Engine


internal object State {
    data class Foo (var i : Int) {
        override fun toString(): String {
            return "Item $i"
        }
    }


    class SelectionState {
        var items = listOf(Foo(1), Foo(2))
        var selection = items[0]
    }

    var selectionState = SelectionState()
}

fun main() {
    val engine = Engine(
        mutableListOf(),
        mutableListOf(WindowingSystemComponent)
    )



    engine.world.attachData(WindowingSystem().apply {
        registerStandardComponents()
//        createWidget().apply {
//            x = WidgetPosition.Fixed(100)
//            y = WidgetPosition.Fixed(100)
//            width = WidgetDimensions.Fixed(200)
//            height = WidgetDimensions.Fixed(100)
//            identifier = "Child A"
//            background.color = bindable(RGBA(180u,180u,180u,255u))
//        }
//
//        createWidget().apply {
//            x = WidgetPosition.Fixed(150)
//            y = WidgetPosition.Fixed(300)
//            width = WidgetDimensions.Intrinsic()
//            height = WidgetDimensions.Intrinsic()
//            identifier = "Child B"
//            background.color = bindable(RGBA(200u, 200u, 200u, 255u))
//            padding = Vec3i(4,4,0)
//            attachData(
//                TextDisplay(
//                text = bindable(RichText("Test"))
//            )
//            )
//        }

        val childA = createWidget("Widgets.ChildA")
        val childB = createWidget("Widgets.ChildB")
        val list = createWidget("Widgets.ListThing")

        childB.bind("test", "Test Text")

        list.bind("listItems", listOf(mapOf("text" to "List item A"), mapOf("text" to "Second list item")))

        desktop.bind("dropdownState", State.selectionState)

        desktop.onEventDo<WidgetMouseReleaseEvent> { e ->
            println("Widget mouse release : ${e.position} <- ${e.from.position}")
            childB.bind("test", "Mouse Release : ${e.position}")
        }
        desktop.onEventDo<DropdownSelectionChanged> {
            println("Dropdown selection changed : ${State.selectionState.selection}")
        }
    })

    Application()
        .run(engine)
}