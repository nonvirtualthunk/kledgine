package arx.display.windowing

import arx.core.Vec2i
import arx.core.Vec3i
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class WindowingSystemTest {

    @Test
    internal fun testWindowingSystemPositioning() {

        val ws = WindowingSystem()

        val windowA = ws.createWidget().apply {
            x = WidgetPosition.Fixed(20)
            y = WidgetPosition.Proportional(0.5f)
            width = WidgetDimensions.Proportional(0.5f)
            height = WidgetDimensions.ExpandToParent(100)
            identifier = "Window A"
        }

        val childA = ws.createWidget(windowA).apply {
            x = WidgetPosition.Proportional(0.1f, WidgetOrientation.BottomRight)
            y = WidgetPosition.Fixed(10, WidgetOrientation.BottomRight)
            width = WidgetDimensions.Fixed(100)
            height = WidgetDimensions.Relative(100)
        }

        val windowB = ws.createWidget().apply {
            x = WidgetPosition.Fixed(100)
            y = WidgetPosition.Fixed(100)
            width = WidgetDimensions.WrapContent
            height = WidgetDimensions.WrapContent
            identifier = "Window B"
        }

        val childB1 = ws.createWidget(windowB).apply {
            x = WidgetPosition.Fixed(10)
            y = WidgetPosition.Fixed(10)
            width = WidgetDimensions.Proportional(1.0f)
            height = WidgetDimensions.Fixed(100)
        }

        val childB2 = ws.createWidget(windowB).apply {
            x = WidgetPosition.Fixed(0)
            y = WidgetPosition.Proportional(0.5f)
            width = WidgetDimensions.Fixed(100)
            height = WidgetDimensions.Relative(90)
        }

        ws.updateGeometry(Vec2i(1200,800))

        assertEquals(Vec3i(20, 400, 0), windowA.resolvedPosition)
        assertEquals(Vec2i(600, 300), windowA.resolvedDimensions)

        assertEquals(Vec2i(100, windowA.resHeight - 100), childA.resolvedDimensions)
        val childAExpectedPos = Vec3i(
            (20 + windowA.resWidth - windowA.resWidth * 0.1 - childA.resWidth).toInt(),
            windowA.resY + windowA.resHeight - 10 - childA.resHeight,
            0
        )
        assertEquals(childAExpectedPos, childA.resolvedPosition)


        assertEquals(Vec3i(100, 100, 0), windowB.resolvedPosition)
        assertEquals(Vec3i(110, 110, 0), childB1.resolvedPosition)

        assertEquals(Vec2i(100, 110), windowB.resolvedDimensions)
        assertEquals(Vec2i(100, 100), childB1.resolvedDimensions)
        assertEquals(Vec3i(windowB.resX + 0, windowB.resY + 55, 0), childB2.resolvedPosition)
        assertEquals(Vec2i(100, 20), childB2.resolvedDimensions)

    }
}