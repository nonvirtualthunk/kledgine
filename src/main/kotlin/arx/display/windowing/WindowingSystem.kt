package arx.display.windowing

import arx.application.Application
import arx.core.*
import arx.display.core.Key
import arx.display.core.KeyModifiers
import arx.display.core.MouseButton
import arx.display.windowing.RecalculationFlag.*
import arx.display.windowing.components.BackgroundComponent
import arx.display.windowing.components.TextWindowingComponent
import arx.engine.*
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.inverse
import dev.romainguy.kotlin.math.ortho
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.lang.Integer.max
import java.lang.Integer.min
import java.lang.ref.WeakReference
import java.util.*


abstract class WidgetEvent(srcEvent: DisplayEvent) : DisplayEvent(srcEvent) {
    abstract val widgets: MutableList<Widget>

    val widget : Widget get() { return widgets.last() }
}

data class WidgetKeyReleaseEvent(override val widgets: MutableList<Widget>, val key: Key, val mods: KeyModifiers, val from: KeyReleaseEvent) : WidgetEvent(from)
data class WidgetKeyPressEvent(override val widgets: MutableList<Widget>, val key: Key, val mods: KeyModifiers, val from: KeyPressEvent) : WidgetEvent(from)
data class WidgetKeyRepeatEvent(override val widgets: MutableList<Widget>, val key: Key, val mods: KeyModifiers, val from: KeyRepeatEvent) : WidgetEvent(from)
data class WidgetMousePressEvent(override val widgets: MutableList<Widget>, val position: Vec2f, val button: MouseButton, val mods: KeyModifiers, val from: MousePressEvent) : WidgetEvent(from)
data class WidgetMouseReleaseEvent(override val widgets: MutableList<Widget>, val position: Vec2f, val button: MouseButton, val mods: KeyModifiers, val from: MouseReleaseEvent) : WidgetEvent(from)
data class WidgetMouseMoveEvent(override val widgets: MutableList<Widget>, val position: Vec2f, val delta: Vec2f, val mods: KeyModifiers, val from: MouseMoveEvent) : WidgetEvent(from)
data class WidgetMouseDragEvent(override val widgets: MutableList<Widget>, val position: Vec2f, val delta: Vec2f, val button: MouseButton, val mods: KeyModifiers, val from: MouseDragEvent) : WidgetEvent(from)


interface WindowingComponent {
    fun registerDataTypes(world: World) {}
    fun intrinsicSize(w: Widget, axis: Axis2D, minSize: Vec2i, maxSize: Vec2i): Int? {
        return null
    }

    fun clientOffsetContribution(w: Widget, axis: Axis2D): Int? {
        return null
    }

    fun render(ws: WindowingSystem, w: Widget, bounds: Recti, quadsOut: MutableList<WQuad>) {

    }

    fun handleEvent(w: Widget, event: DisplayEvent) : Boolean { return false }
}

class WindowingSystem {
    val world = World()
    val desktop = Widget(this, null).apply { identifier = "Desktop" }
    val widgets = mutableMapOf(desktop.entity.id to desktop)
    var focusedWidget : Widget? = null
    var scale = 3

    var pendingUpdates = mutableMapOf<Widget, EnumSet<RecalculationFlag>>(desktop to EnumSet.allOf(RecalculationFlag::class.java))

    private var components = mutableListOf<WindowingComponent>()

    fun registerComponent(c: WindowingComponent) {
        components.add(c)
        c.registerDataTypes(world)
    }

    fun registerStandardComponents() {
        registerComponent(BackgroundComponent)
        registerComponent(TextWindowingComponent)
    }

    fun createWidget(): Widget {
        return createWidget(desktop)
    }

    fun createWidget(parent: Widget): Widget {
        val w = Widget(this, parent)
        widgets[w.entity.id] = w
        markForFullUpdate(w)
        return w
    }

    fun markForUpdate(w: Widget, r: RecalculationFlag) {
        pendingUpdates.getOrPut(w) { EnumSet.noneOf(RecalculationFlag::class.java) }.add(r)
    }

    fun markForFullUpdate(w: Widget) {
        pendingUpdates.getOrPut(w) { EnumSet.allOf(RecalculationFlag::class.java) }.addAll(RecalculationFlag.values())
    }

    fun updateDrawData(w: Widget, bounds: Recti, needsRerender: Set<Widget>) {
        w.bounds = bounds
        if (needsRerender.contains(w)) {
            w.quads.clear()
            for (comp in components) {
                comp.render(this, w, bounds, w.quads)
            }
        }

        val cx = w.clientOffset(Axis.X)
        val cy = w.clientOffset(Axis.Y)
        val newBounds = bounds.intersect(Recti(w.resX + cx, w.resY + cy, w.resWidth - cx * 2, w.resHeight - cy * 2))
        for (c in w.children) {
            updateDrawData(c, newBounds, needsRerender)
        }
    }


    fun updateGeometry(size: Vec2i): Boolean {
        updateDesktop(size)

        updateDependentRelationships()

        val requireRerender = ObjectOpenHashSet<Widget>()
        val requiredUpdates = recursivelyCollectRequiredDependencies(requireRerender)
        val completedUpdates = DependencySet(requiredUpdates.size)

        requiredUpdates.forEach { update -> processGeometryUpdate(update, requiredUpdates, completedUpdates, requireRerender) }

        pendingUpdates.clear()

        return if (requireRerender.isNotEmpty()) {
            updateDrawData(desktop, Recti(-100000, -100000, 200000, 200000), requireRerender)
            true
        } else {
            false
        }
    }

    fun updateDesktop(size: Vec2i) {
        val desiredSize = (size) / scale
        if (desktop.resolvedDimensions != desiredSize) {
            desktop.resolvedDimensions = desiredSize
            markForUpdate(desktop, DimensionsX)
            markForUpdate(desktop, DimensionsY)
        }
    }


    fun Dependency.widget(): Widget? {
        return widgets[widgetId]
    }

    fun processGeometryUpdate(d: Dependency, requiredUpdates: DependencySet, completedUpdates: DependencySet, requireRerender: ObjectOpenHashSet<Widget>) {
        d.widget()?.let { w ->
            when (d.kind) {
                DependencyKind.PartialPosition -> updatePartialPosition(w, d.axis, requiredUpdates, completedUpdates, requireRerender)
                DependencyKind.Position -> updatePosition(w, d.axis, requiredUpdates, completedUpdates, requireRerender)
                DependencyKind.Dimensions -> updateDimensions(w, d.axis, requiredUpdates, completedUpdates, requireRerender, false)
            }
        }
    }


    fun updateClientOffset(w: Widget, axis: Axis) {
        axis.to2D()?.let { ax2d ->
            var newValue = 0
            for (comp in components) {
                newValue += comp.clientOffsetContribution(w, ax2d) ?: 0
            }

            w.resolvedClientOffset[axis] = newValue
        }
    }

    /**
     * Partial position is effectively: the position we can estimate without requiring information about
     * our parent or complex layout considerations. Necessary in order to make things like WrapContent
     * function along with right aligned widgets. Can be best effort, should try to avoid requiring
     * resolving other dependencies, where necessary only partial position should be used if possible
     */
    fun updatePartialPosition(w: Widget, axis: Axis, requiredUpdates: DependencySet, completedUpdates: DependencySet, requireRerender: ObjectOpenHashSet<Widget>) {
        // only perform the update if we haven't already (completedUpdates) and an update is actually required (requiredUpdates)
        if (completedUpdates.add(w, DependencyKind.PartialPosition, axis) && requiredUpdates.contains(w, DependencyKind.PartialPosition, axis)) {
            // only the top level Desktop widget should have no parent, it is handled differently, so only
            // process for parent'd widgets
            w.parent?.let { parent ->
                val pos = w.position(axis)
                w.resolvedPartialPosition[axis] = when (pos) {
                    is WidgetPosition.Fixed -> if (isFarSide(axis, pos.relativeTo)) 0 else pos.offset
                    is WidgetPosition.Proportional -> 0
                    is WidgetPosition.Centered -> 0
                    is WidgetPosition.Relative -> {
                        val relativeWidget = pos.relativeTo.toWidget()
                        if (relativeWidget != null) {
                            updatePartialPosition(relativeWidget, axis, requiredUpdates, completedUpdates, requireRerender)
                            val relativeAnchor = if (isFarSide(axis, pos.targetAnchor)) {
                                updateDimensions(relativeWidget, axis, requiredUpdates, completedUpdates, requireRerender)
                                relativeWidget.resolvedPartialPosition[axis] + relativeWidget.resolvedDimensions[axis] + pos.offset
                            } else {
                                relativeWidget.resolvedPartialPosition[axis] - pos.offset
                            }

                            if (isFarSide(axis, pos.selfAnchor)) {
                                updateDimensions(w, axis, requiredUpdates, completedUpdates, requireRerender)
                                relativeAnchor - w.resolvedDimensions[axis]
                            } else {
                                relativeAnchor
                            }
                        } else {
                            0
                        }
                    }
                    is WidgetPosition.Absolute -> pos.position
                }
            }
        }
    }

    fun updatePosition(w: Widget, axis: Axis, requiredUpdates: DependencySet, completedUpdates: DependencySet, requireRerender: ObjectOpenHashSet<Widget>) {
        // only perform the update if we haven't already (completedUpdates) and an update is actually required (requiredUpdates)
        if (completedUpdates.add(w, DependencyKind.Position, axis) && requiredUpdates.contains(w, DependencyKind.Position, axis)) {
            w.parent?.let { parent ->
                val pos = w.position(axis)

                updateClientOffset(w, axis)
                // everything other than absolute pixel positions and positions relative
                // to widgets other than the parent require the parent's position
                if (pos !is WidgetPosition.Absolute && pos !is WidgetPosition.Relative) {
                    updatePosition(parent, axis, requiredUpdates, completedUpdates, requireRerender)
                }
                val parentV = parent.resolvedPosition[axis] + parent.clientOffset(axis)

                val newPos = when (pos) {
                    is WidgetPosition.Fixed -> {
                        if (isFarSide(axis, pos.relativeTo)) {
                            updateDimensions(w, axis, requiredUpdates, completedUpdates, requireRerender)
                            updateDimensions(parent, axis, requiredUpdates, completedUpdates, requireRerender)
                            val parentD = if (axis.is2D()) parent.resolvedDimensions[axis] - parent.clientOffset(axis) * 2 else 1000
                            parentV + parentD - pos.offset - w.resolvedDimensions[axis]
                        } else {
                            parentV + pos.offset
                        }
                    }
                    is WidgetPosition.Proportional -> {
//                        updatePosition(parent, axis, requiredUpdates, completedUpdates)
                        if (isFarSide(axis, pos.relativeTo) || pos.anchorToCenter) {
                            updateDimensions(w, axis, requiredUpdates, completedUpdates, requireRerender)
                        }
                        updateDimensions(parent, axis, requiredUpdates, completedUpdates, requireRerender)
                        val parentD = if (axis.is2D()) parent.resolvedDimensions[axis] - parent.clientOffset(axis) * 2 else 1000
                        val primaryPoint = if (isFarSide(axis, pos.relativeTo)) {
                            parentV + parentD - (parentD.toFloat() * pos.proportion).toInt() - w.resolvedDimensions[axis]
                        } else {
                            parentV + (parentD.toFloat() * pos.proportion).toInt()
                        }

                        if (pos.anchorToCenter) {
                            if (isFarSide(axis, pos.relativeTo)) {
                                primaryPoint - w.resolvedDimensions[axis]
                            } else {
                                primaryPoint + w.resolvedDimensions[axis]
                            }
                        } else {
                            primaryPoint
                        }
                    }
                    is WidgetPosition.Centered -> {
                        updatePosition(parent, axis, requiredUpdates, completedUpdates, requireRerender)
                        updateDimensions(parent, axis, requiredUpdates, completedUpdates, requireRerender)
                        updateDimensions(w, axis, requiredUpdates, completedUpdates, requireRerender)
                        val parentD = if (axis.is2D()) parent.resolvedDimensions[axis] - parent.clientOffset(axis) * 2 else 1000
                        parentV + (parentD - w.resolvedDimensions[axis]) / 2
                    }
                    is WidgetPosition.Relative -> {
                        val relativeWidget = pos.relativeTo.toWidget()
                        if (relativeWidget != null) {
                            updatePosition(relativeWidget, axis, requiredUpdates, completedUpdates, requireRerender)
                            val relativeAnchor = if (isFarSide(axis, pos.targetAnchor)) {
                                updateDimensions(relativeWidget, axis, requiredUpdates, completedUpdates, requireRerender)
                                relativeWidget.resolvedPosition[axis] + relativeWidget.resolvedDimensions[axis] + pos.offset
                            } else {
                                relativeWidget.resolvedPosition[axis] - pos.offset
                            }

                            if (isFarSide(axis, pos.selfAnchor)) {
                                updateDimensions(w, axis, requiredUpdates, completedUpdates, requireRerender)
                                relativeAnchor - w.resolvedDimensions[axis]
                            } else {
                                relativeAnchor
                            }
                        } else {
                            0
                        }
                    }
                    is WidgetPosition.Absolute -> {
                        if (pos.anchorTo == WidgetOrientation.Center) {
                            updateDimensions(w, axis, requiredUpdates, completedUpdates, requireRerender)
                            pos.position - w.resolvedDimensions[axis] / 2
                        } else if (isFarSide(axis, pos.anchorTo)) {
                            updateDimensions(w, axis, requiredUpdates, completedUpdates, requireRerender)
                            pos.position - w.resolvedDimensions[axis]
                        } else {
                            pos.position
                        }
                    }
                }
                if (newPos != w.resolvedPosition[axis]) {
                    w.resolvedPosition[axis] = newPos
                    requireRerender.add(w)
                }
            }
        }
    }

    fun updateDimensions(w: Widget, axis: Axis, requiredUpdates: DependencySet, completedUpdates: DependencySet, requireRerender: ObjectOpenHashSet<Widget>, ignoreIfDependsOnParent: Boolean = false) {
        val dim = w.dimensions(axis)
        if (ignoreIfDependsOnParent && dim.dependsOnParent()) {
            return
        }

        // only perform the update if we haven't already (completedUpdates) and an update is actually required (requiredUpdates)
        if (completedUpdates.add(w, DependencyKind.Dimensions, axis) && requiredUpdates.contains(w, DependencyKind.Dimensions, axis)) {
            w.parent?.let { parent ->

                updateClientOffset(w, axis)

                if (dim is WidgetDimensions.Relative || dim is WidgetDimensions.Proportional || dim is WidgetDimensions.ExpandToParent) {
                    updateDimensions(parent, axis, requiredUpdates, completedUpdates, requireRerender)
                }
                val parentD = parent.resolvedDimensions[axis] - parent.clientOffset(axis) * 2

                val newDim = when (dim) {
                    is WidgetDimensions.Fixed -> dim.size
                    is WidgetDimensions.Relative -> parentD - dim.delta
                    is WidgetDimensions.Proportional -> (parentD.toFloat() * dim.proportion).toInt()
                    is WidgetDimensions.ExpandToParent -> {
                        updatePosition(parent, axis, requiredUpdates, completedUpdates, requireRerender)
                        updatePosition(w, axis, requiredUpdates, completedUpdates, requireRerender)
                        val relPos = w.resolvedPosition[axis] - (parent.resolvedPosition[axis] + parent.clientOffset(axis))
                        parentD - dim.gap - relPos
                    }
                    is WidgetDimensions.ExpandTo -> {
                        val expandTo = dim.expandTo.toWidget()
                        if (expandTo != null) {
                            updatePosition(expandTo, axis, requiredUpdates, completedUpdates, requireRerender)
                            expandTo.resolvedPosition[axis] - w.resolvedPosition[axis] - dim.gap
                        } else {
                            0
                        }
                    }
                    WidgetDimensions.WrapContent -> {
                        var min = 10000000
                        var max = 0
                        for (c in w.children) {
                            updatePartialPosition(c, axis, requiredUpdates, completedUpdates, requireRerender)
                            updateDimensions(c, axis, requiredUpdates, completedUpdates, requireRerender, ignoreIfDependsOnParent = true)
                            min = min(c.resolvedPartialPosition[axis], min)
                            max = max(c.resolvedPartialPosition[axis] + c.resolvedDimensions[axis], max)
                        }

                        max(max - min, 0) + w.clientOffset(axis)
                    }
                    is WidgetDimensions.Intrinsic -> {
                        val minimums = Vec2i(0, 0)
                        val maximums = Vec2i(1000000000, 1000000000)
                        if (w.dimensions(oppositeAxis(axis)) !is WidgetDimensions.Intrinsic) {
                            updateDimensions(w, oppositeAxis(axis), requiredUpdates, completedUpdates, requireRerender)
                        }
                        val xdim = w.dimensions(Axis.X)
                        val ydim = w.dimensions(Axis.Y)
                        if (xdim is WidgetDimensions.Intrinsic) {
                            minimums.x = xdim.min ?: 0
                            maximums.x = xdim.max ?: 1000000000
                        } else {
                            maximums.x = w.resolvedDimensions[Axis.X]
                        }
                        if (ydim is WidgetDimensions.Intrinsic) {
                            minimums.y = ydim.min ?: 0
                            maximums.y = ydim.max ?: 1000000000
                        } else {
                            maximums.y = w.resolvedDimensions[Axis.Y]
                        }

                        var result = 10
                        axis.to2D()?.let { ax2d ->
                            for (component in components) {
                                val isize = component.intrinsicSize(w, ax2d, minimums, maximums)
                                if (isize != null) {
                                    result = isize + w.clientOffset(axis) * 2
                                    break
                                }
                            }
                        }
                        result
                    }
                }
                if (newDim != w.resolvedDimensions[axis]) {
                    w.resolvedDimensions[axis] = newDim
                    requireRerender.add(w)
                }
            }
        }
    }


    fun recursivelyCollectRequiredDependencies(requireRerender: ObjectOpenHashSet<Widget>): DependencySet {
        val ret = DependencySet()
        for ((w, flags) in pendingUpdates) {
            for (flag in flags) {
                if (flag == Contents) {
                    // content updates always necessitate re-rendering the widget itself
                    requireRerender.add(w)
                    // and require dimensional updates when the dimensions are based on the content itself
                    for (axis in Axis2D.values()) {
                        if (w.dimensions(axis) is WidgetDimensions.Intrinsic) {
                            recursivelyCollectRequiredDependencies(w, DependencyKind.Dimensions, axis.to3D(), ret)
                        }
                    }
                } else {
                    // convert the update flag into the appropriate dependency representation, then
                    // recursively collect dependencies based on it. Could we just always use the
                    // dependency representation rather than having the two different ones? Probably?
                    val (dw, dk, da) = when (flag) {
                        PositionX -> Triple(w, DependencyKind.Position, Axis.X)
                        PositionY -> Triple(w, DependencyKind.Position, Axis.Y)
                        PositionZ -> Triple(w, DependencyKind.Position, Axis.Z)
                        DimensionsX -> Triple(w, DependencyKind.Dimensions, Axis.X)
                        DimensionsY -> Triple(w, DependencyKind.Dimensions, Axis.Y)
                        else -> continue
                    }
                    recursivelyCollectRequiredDependencies(dw, dk, da, ret)
                    if (dk == DependencyKind.Position) {
                        recursivelyCollectRequiredDependencies(dw, DependencyKind.PartialPosition, da, ret)
                    }
                }
            }
        }
        return ret
    }

    fun recursivelyCollectRequiredDependencies(w: Widget, kind: DependencyKind, axis: Axis, resultSet: DependencySet) {
        if (resultSet.add(Dependency(w, kind, axis))) {
            w.forEachDependent { dependent ->
                if (dependent.sourceAxis == axis && dependent.sourceKind == kind) {
                    dependent.dependentWidget.get()?.let { dw ->
                        recursivelyCollectRequiredDependencies(dw, dependent.dependentKind, dependent.dependentAxis, resultSet)
                    }
                }
            }
        }
    }

    fun updateDependentRelationships() {
        // Note: this makes dependency purely additive, a dependent relationship
        // cannot currently be removed
        for ((w, flags) in pendingUpdates) {
            if (flags.contains(DimensionsX)) {
                forEachDimensionDep(w, Axis.X) { depW, k, a -> depW.addDependent(Dependent(WeakReference(w), DependencyKind.Dimensions, Axis.X, k, a)) }
            }
            if (flags.contains(DimensionsY)) {
                forEachDimensionDep(w, Axis.Y) { depW, k, a -> depW.addDependent(Dependent(WeakReference(w), DependencyKind.Dimensions, Axis.Y, k, a)) }
            }
            if (flags.contains(PositionX)) {
                forEachPositionDep(w, Axis.X) { depW, k, a -> depW.addDependent(Dependent(WeakReference(w), DependencyKind.Position, Axis.X, k, a)) }
            }
            if (flags.contains(PositionY)) {
                forEachPositionDep(w, Axis.Y) { depW, k, a -> depW.addDependent(Dependent(WeakReference(w), DependencyKind.Position, Axis.Y, k, a)) }
            }
            if (flags.contains(PositionZ)) {
                forEachPositionDep(w, Axis.Z) { depW, k, a -> depW.addDependent(Dependent(WeakReference(w), DependencyKind.Position, Axis.Z, k, a)) }
            }
        }
    }

    fun widgetContainsPosition(w: Widget, position: Vec2f) : Boolean {
        return w.resX <= position.x && w.resY <= position.y && w.resX + w.resWidth >= position.x && w.resY + w.resHeight >= position.y
    }

    fun widgetUnderMouse(position: Vec2f) : Widget {
        var w = desktop
        while(true) {
            var newW : Widget? = null
            for (c in w.children) {
                if (c.showing() && widgetContainsPosition(c, position)) {
                    newW = c
                    break
                }
            }
            newW?.let { w = it }
            if (newW == null) {
                break
            }
        }
        return w
    }

    private fun handleEvent(w: Widget, event: DisplayEvent) : Boolean {
        if (event is WidgetEvent) {
            event.widgets.add(w)
        }

        for (callback in w.eventCallbacks) {
            if (callback(event)) {
                event.consume()
                return true
            }
        }

        for (comp in components) {
            if (comp.handleEvent(w, event)) {
                event.consume()
                return true
            }
        }
        val p = w.parent
        return if (p != null) {
            handleEvent(p, event)
        } else {
            false
        }
    }

    fun projectionMatrix(): Mat4 {
        return ortho(desktop.resX.toFloat(), (desktop.resX + desktop.resWidth + 1).toFloat(), (desktop.resY + desktop.resHeight).toFloat(), desktop.resY.toFloat(), 0.0f, 100.0f)
    }

    fun screenToW(p : Vec2f) : Vec2f {
        val screenSpace = Float4((p.x / Application.windowSize.x) * 2.0f - 1.0f, ((Application.windowSize.y - p.y - 1) / Application.windowSize.y) * 2.0f - 1.0f, 0.0f, 1.0f)
        val res = inverse(projectionMatrix()).times(screenSpace)
        return Vec2f(res.x, res.y)
    }

    private fun mapEvent(event: DisplayEvent) : WidgetEvent? {
        return when (event) {
            is KeyReleaseEvent -> WidgetKeyReleaseEvent(mutableListOf(), event.key, event.mods, event)
            is KeyPressEvent -> WidgetKeyPressEvent(mutableListOf(), event.key, event.mods, event)
            is KeyRepeatEvent -> WidgetKeyRepeatEvent(mutableListOf(), event.key, event.mods, event)
            is MousePressEvent -> WidgetMousePressEvent(mutableListOf(), screenToW(event.position), event.button, event.mods, event)
            is MouseReleaseEvent -> WidgetMouseReleaseEvent(mutableListOf(), screenToW(event.position), event.button, event.mods, event)
            is MouseMoveEvent -> WidgetMouseMoveEvent(mutableListOf(), screenToW(event.position), event.delta, event.mods, event)
            is MouseDragEvent -> WidgetMouseDragEvent(mutableListOf(), screenToW(event.position), event.delta, event.button, event.mods, event)
            else -> null
        }
    }

    /**
     * Process the given event through the various widgets in the windowing system. Returns true
     * if one of those widgets consumed the event, false otherwise (including if the event was
     * already consumed before this was called).
     */
    fun handleEvent(event : DisplayEvent) : Boolean {
        if (event.consumed) { return false }

        val target = when (event) {
            is MouseEvent -> widgetUnderMouse(screenToW(event.position))
            is KeyEvent -> focusedWidget ?: desktop
            else -> focusedWidget ?: desktop
        }

        return handleEvent(target, mapEvent(event) ?: event)
    }
}
