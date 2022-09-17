package arx.display.windowing

import arx.core.*
import arx.display.core.Image
import arx.display.core.ImagePath
import arx.display.core.ImageRef
import arx.display.core.RGBA
import arx.display.windowing.RecalculationFlag.*
import arx.engine.*
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

val WidgetIdCounter = AtomicInteger(1)


enum class WidgetOrientation {
    TopLeft,
    BottomRight,
    TopRight,
    BottomLeft,
    Center
}

enum class RecalculationFlag {
    PositionX,
    PositionY,
    PositionZ,
    DimensionsX,
    DimensionsY,
    Contents
}

sealed interface WidgetIdentifier {
    fun toWidget(): Widget?
}

data class WQuad(
    var position: Vec3i,
    var dimensions: Vec2i,
    var image: Image?,
    var color: arx.core.RGBA?,
    var beforeChildren: Boolean,
    var forward: Vec2f? = null,
    var subRect: Rectf? = null
)



/**
 * A widget identified by a name within the context of another widget's view
 * That is, it is intended to look up other widgets that share a parent and
 * have the given name.
 * `context` is the widget that is "interested" in another widget of the given
 * name.
 */
data class WidgetNameIdentifier(val context: Widget, val name: String) : WidgetIdentifier {
    override fun toWidget(): Widget? {
        return context.parent?.children?.find { it.identifier == name }
    }
}

sealed interface WidgetPosition {
    data class Fixed(val offset: Int, val relativeTo: WidgetOrientation = WidgetOrientation.TopLeft) : WidgetPosition
    data class Proportional(val proportion: Float, val relativeTo: WidgetOrientation = WidgetOrientation.TopLeft, val anchorToCenter: Boolean = false) : WidgetPosition
    data class Absolute(val position: Int, val anchorTo: WidgetOrientation = WidgetOrientation.TopLeft) : WidgetPosition
    object Centered : WidgetPosition
    data class Relative(val relativeTo: WidgetIdentifier, val offset: Int, val targetAnchor: WidgetOrientation = WidgetOrientation.BottomRight, val selfAnchor: WidgetOrientation = WidgetOrientation.TopLeft) : WidgetPosition
}

sealed interface WidgetDimensions {
    data class Fixed(val size: Int) : WidgetDimensions
    data class Relative(val delta: Int) : WidgetDimensions
    data class Proportional(val proportion: Float) : WidgetDimensions
    data class ExpandToParent(val gap: Int) : WidgetDimensions
    data class Intrinsic(val max: Int? = null, val min: Int? = null) : WidgetDimensions
    object WrapContent : WidgetDimensions
    data class ExpandTo(val expandTo: WidgetIdentifier, val gap: Int) : WidgetDimensions

    fun dependsOnParent(): Boolean {
        return when (this) {
            is Intrinsic -> false
            is WrapContent -> false
            is Fixed -> false
            else -> true
        }
    }

    fun isIntrinsic(): Boolean {
        return when(this) {
            is Intrinsic -> true
            else -> false
        }
    }
}

enum class DependencyKind {
    PartialPosition,
    Position,
    Dimensions
}
fun dependencyKind(l : Long) : DependencyKind {
    return when (l) {
        0L -> DependencyKind.PartialPosition
        1L -> DependencyKind.Position
        2L -> DependencyKind.Dimensions
        else -> error("Invalid dependency kind ordinal: $l")
    }
}

//data class Dependency(var widget: Widget, var kind: DependencyKind, var axis: Axis) {
//    companion object {
//        const val AxisMask = ((1 shl 2) - 1).toLong()
//        const val KindMask = ((1 shl 2) - 1).toLong()
//    }
//}

@JvmInline
value class Dependency(val packed: Long) {
    constructor (widget: Widget, kind: DependencyKind, axis: Axis) :
            this(axis.ordinal.toLong() or (kind.ordinal shl 2).toLong() or (widget.entity.id shl 4).toLong())

    companion object {
        const val AxisMask = ((1 shl 2) - 1).toLong()
        const val AxisShift = 0
        const val KindMask = ((1 shl 2) - 1).toLong()
        const val KindShift = AxisShift + 2
        const val WidgetShift = KindShift + 2
    }
    val axisOrd : Int get() { return (packed and AxisMask).toInt() }
    val axis : Axis get() { return axis((packed and AxisMask).toInt()) }
    val kindOrd : Int get() { return ((packed shr KindShift) and KindMask).toInt() }
    val kind : DependencyKind get() { return dependencyKind((packed shr KindShift) and KindMask) }
    val widgetId : Int get() { return (packed shr WidgetShift).toInt() }
    fun isEmpty() : Boolean { return packed == 0L }

    override fun toString(): String {
        return "Dependency(widget: $widgetId, kind: $kind, axis: $axis)"
    }
}


/**
 * Represents a dependent relationship. The widget containing this is dependent
 * on another widget's Position/PartialPosition/Dimensions in a particular axis
 * in order to be able to calculate its own Position/etc on an axis
 */
data class Dependent(
    var dependentWidget: WeakReference<Widget>,
    var dependentKind: DependencyKind,
    var dependentAxis: Axis,
    var sourceKind: DependencyKind,
    var sourceAxis: Axis
)


class DependencySet(initialSize: Int = 1024) {
    val intern = LongOpenHashSet(initialSize)

    fun add(d: Dependency) : Boolean {
        return intern.add(d.packed)
    }
    fun add(w: Widget, k: DependencyKind, axis: Axis) : Boolean {
        return intern.add(Dependency(w,k,axis).packed)
    }
    fun contains(d: Dependency) : Boolean {
        return intern.contains(d.packed)
    }
    fun contains(w: Widget, k: DependencyKind, axis: Axis) : Boolean {
        return intern.contains(Dependency(w,k,axis).packed)
    }

    fun forEach(fn : (Dependency) -> Unit) {
        val iter = intern.iterator()
        while (iter.hasNext()) {
            fn(Dependency(iter.nextLong()))
        }
    }

    override fun toString(): String {
        val joinedDependencies = intern.joinToString("\n") { l -> "\t" + Dependency(l).toString() }
        return """DependencySet[
    $joinedDependencies
"""
    }

    val size get() : Int { return intern.size }
}

data class NineWayImage(
    var image: Bindable<ImageRef>,
    var scale: Int = 1,
    var draw: Bindable<Boolean> = ValueBindable.True,
    var drawCenter: Bindable<Boolean> = ValueBindable.True,
    var drawEdges: Bindable<Boolean> = ValueBindable.True,
    var color: Bindable<RGBA?> = ValueBindable.Null(),
    var edgeColor: Bindable<RGBA?> = ValueBindable.Null(),
)

class Widget(val windowingSystem: WindowingSystem, var parent: Widget?) : WidgetIdentifier {
    init {
        parent?.addChild(this)
    }
    var identifier: String? = null
    val entity: Entity = windowingSystem.world.createEntity()
    private var childrenI: MutableList<Widget>? = null
    val children : List<Widget> get() {
        if (childrenI != null && childSortNeeded) {
            childrenI!!.sortBy { c -> -c.resZ }
        }
        return childrenI ?: emptyList()
    }
    private var childSortNeeded = false
    private fun addChild(w: Widget) {
        if (childrenI == null) {
            childrenI = mutableListOf()
        }
        childrenI!!.add(w)
        childSortNeeded = true
    }

    internal var dependents = mutableSetOf<Dependent>()

    internal var positionI = Vec3(WidgetPosition.Fixed(0), WidgetPosition.Fixed(0), WidgetPosition.Absolute(0))
    internal var dimensionsI = Vec2<WidgetDimensions>(WidgetDimensions.Fixed(100), WidgetDimensions.Fixed(100))
    var padding = Vec3i()

    var showing : Bindable<Boolean> = ValueBindable.True
    var resolvedPosition = Vec3i()
    var resolvedPartialPosition = Vec3i()
    var resolvedDimensions = Vec2i()
    var resolvedClientOffset = Vec3i()

    val bindings = mutableMapOf<String, Any>()
    var eventCallbacks : List<(DisplayEvent) -> Boolean> = emptyList()

    var destroyed: Boolean = false


    // +============================================================================+
    // |                            Drawing                                         |
    // +============================================================================+

    val background = NineWayImage(bindable(ImagePath("arx/ui/minimalistBorder.png")))
    val overlay : NineWayImage? = null


    val quads = mutableListOf<WQuad>()
    var bounds = Recti(0,0,0,0)

    // +============================================================================+
    // |                            Access                                          |
    // +============================================================================+

    fun markForUpdate(d: RecalculationFlag) {
        windowingSystem.markForUpdate(this, d)
    }

    fun position(axis: Axis) : WidgetPosition { return positionI[axis] }
    fun dimensions(axis: Axis) : WidgetDimensions { return dimensionsI[axis] }
    fun dimensions(axis: Axis2D) : WidgetDimensions { return dimensionsI[axis] }
    fun clientOffset(axis: Axis) : Int { return resolvedClientOffset[axis] + padding[axis] }

    var x : WidgetPosition get() { return positionI.x } set(v) { markForUpdate(PositionX); positionI.x = v }
    var y : WidgetPosition get() { return positionI.y } set(v) { markForUpdate(PositionY); positionI.y = v }
    var z : WidgetPosition get() { return positionI.z } set(v) { markForUpdate(PositionZ); positionI.z = v }

    var width : WidgetDimensions get() { return dimensionsI.x } set(v) { markForUpdate(DimensionsX); dimensionsI.x = v }
    var height : WidgetDimensions get() { return dimensionsI.y } set(v) { markForUpdate(DimensionsY); dimensionsI.y = v }


    val resX : Int get() { return resolvedPosition.x }
    val resY : Int get() { return resolvedPosition.y }
    val resClientX : Int get() { return resolvedPosition.x + clientOffset(Axis.X) }
    val resClientY : Int get() { return resolvedPosition.y + clientOffset(Axis.Y) }
    val resZ : Int get() { return resolvedPosition.z }
    val resWidth : Int get() { return resolvedDimensions.x }
    val resHeight : Int get() { return resolvedDimensions.y }
    val resClientWidth : Int get() { return resolvedDimensions.x - clientOffset(Axis.X) * 2 }
    val resClientHeight : Int get() { return resolvedDimensions.y - clientOffset(Axis.Y) * 2 }

    operator fun <T : EntityData> get (dt: DataType<T>) : T? {
        return windowingSystem.world.data(entity, dt)
    }

    fun <T : EntityData> attachData(t: T) {
        windowingSystem.world.attachData(entity, t)
    }

    fun addDependent(dep : Dependent) {
        dependents.add(dep)
    }

    fun removeDependent(dep: Dependent) {
        dependents.remove(dep)
    }

    internal fun forEachDependent(fn: (Dependent) -> Unit) {
        for (dep in dependents) {
            fn(dep)
        }
    }

    fun onEvent(stmt: (DisplayEvent) -> Boolean) {
        eventCallbacks = eventCallbacks + stmt
    }

    override fun toWidget(): Widget {
        return this
    }

    override fun toString(): String {
        return "Widget(${identifier ?: entity.id})"
    }
}

inline fun <reified T : DisplayEvent> Widget.onEvent(crossinline stmt: (T) -> Boolean) {
    val callback = { d : DisplayEvent ->
        if (d is T) {
            stmt(d)
        } else {
            false
        }
    }
    onEvent(callback)
}

inline fun <reified T : DisplayEvent> Widget.onEventDo(crossinline stmt: (T) -> Unit) {
    val callback = { d : DisplayEvent ->
        if (d is T) {
            stmt(d)
        }
        false
    }
    onEvent(callback)
}



fun isFarSide(axis: Axis, orientation: WidgetOrientation): Boolean {
    return (axis == Axis.X && (orientation == WidgetOrientation.TopRight || orientation == WidgetOrientation.BottomRight)) ||
            (axis == Axis.Y && (orientation == WidgetOrientation.BottomRight || orientation == WidgetOrientation.BottomLeft))
}

internal fun oppositeAxis(axis: Axis) : Axis {
    return when (axis) {
        Axis.X -> Axis.Y
        Axis.Y -> Axis.X
        else -> error("Only can take opposite of 2d axes")
    }
}


inline fun forEachPositionDep(w: Widget, axis: Axis, fn : (Widget, DependencyKind, Axis) -> Unit) {
    w.parent?.let { parent ->
        val p = w.position(axis)

        when (p) {
            is WidgetPosition.Fixed -> {
                fn(parent, DependencyKind.Position, axis)
                if (isFarSide(axis, p.relativeTo)) {
                    fn(parent, DependencyKind.Dimensions, axis)
                    fn(w, DependencyKind.Dimensions, axis)
                }
            }
            is WidgetPosition.Proportional -> {
                fn(parent, DependencyKind.Position, axis)
                fn(parent, DependencyKind.Dimensions, axis)
                if (isFarSide(axis, p.relativeTo) || p.relativeTo == WidgetOrientation.Center) {
                    fn(w, DependencyKind.Dimensions, axis)
                }
            }
            WidgetPosition.Centered -> {
                fn(parent, DependencyKind.Dimensions, axis)
                fn(w, DependencyKind.Dimensions, axis)
                fn(parent, DependencyKind.Position, axis)
            }
            is WidgetPosition.Relative -> {
                val rel = p.relativeTo.toWidget()
                if (rel != null) {
                    fn(rel, DependencyKind.Position, axis)
                    if (isFarSide(axis, p.targetAnchor) || p.targetAnchor == WidgetOrientation.Center) {
                        fn(rel, DependencyKind.Dimensions, axis)
                    }
                    if (isFarSide(axis, p.selfAnchor) || p.selfAnchor == WidgetOrientation.Center) {
                        fn(w, DependencyKind.Dimensions, axis)
                    }
                } else {
                    Noto.warn("Relative widget position could not resolve relativeTo: ${p.relativeTo}")
                }
            }
            is WidgetPosition.Absolute -> {
                if (isFarSide(axis, p.anchorTo) || p.anchorTo == WidgetOrientation.Center) {
                    fn(w, DependencyKind.Dimensions, axis)
                }
            }
        }
    }
}

internal inline fun forEachDimensionDep(w: Widget, axis: Axis, fn : (Widget, DependencyKind, Axis) -> Unit) {
    w.parent?.let { parent ->
        if (axis == Axis.X || axis == Axis.Y) {
            val d = w.dimensions(axis)
            when (d) {
                is WidgetDimensions.Proportional -> fn(parent, DependencyKind.Dimensions, axis)
                is WidgetDimensions.Relative -> fn(parent, DependencyKind.Dimensions, axis)
                is WidgetDimensions.ExpandToParent -> {
                    fn(parent, DependencyKind.Dimensions, axis)
                    fn(parent, DependencyKind.Position, axis)
                    fn(w, DependencyKind.Position, axis)
                }
                is WidgetDimensions.Intrinsic -> {
                    if (w.dimensions(oppositeAxis(axis)) !is WidgetDimensions.Intrinsic) {
                        fn(w, DependencyKind.Dimensions, oppositeAxis(axis))
                    }
                }
                is WidgetDimensions.WrapContent -> {
                    for (c in w.children) {
                        fn(c, DependencyKind.PartialPosition, axis)
                        fn(c, DependencyKind.Dimensions, axis)
                    }
                }
                is WidgetDimensions.ExpandTo -> {
                    val expandTo = d.expandTo.toWidget()
                    if (expandTo != null) {
                        fn(expandTo, DependencyKind.Position, axis)
                        fn(expandTo, DependencyKind.Dimensions, axis)
                    }
                }
                is WidgetDimensions.Fixed -> {}
            }
        }
    }
}


//internal fun dependenciesFor(w: Widget, axis: Axis): Sequence<Dependency> {
//    return sequence {
//        w.parent?.let { parent ->
//            val p = w.position[axis]
//
//            when (p) {
//                is WidgetPosition.Fixed -> {
//                    yield(Dependency(parent, DependencyKind.Position, axis))
//                    if (isFarSide(axis, p.relativeTo)) {
//                        yield(Dependency(parent, DependencyKind.Dimensions, axis))
//                        yield(Dependency(w, DependencyKind.Dimensions, axis))
//                    }
//                }
//                is WidgetPosition.Proportional -> {
//                    yield(Dependency(parent, DependencyKind.Position, axis))
//                    yield(Dependency(parent, DependencyKind.Dimensions, axis))
//                    if (isFarSide(axis, p.relativeTo) || p.relativeTo == WidgetOrientation.Center) {
//                        yield(Dependency(w, DependencyKind.Dimensions, axis))
//                    }
//                }
//                is WidgetPosition.Centered -> {
//                    yield(Dependency(parent, DependencyKind.Dimensions, axis))
//                    yield(Dependency(w, DependencyKind.Dimensions, axis))
//                    yield(Dependency(parent, DependencyKind.Position, axis))
//                }
//                is WidgetPosition.Relative -> {
//                    yield(Dependency(p.relativeTo, DependencyKind.Position, axis))
//                    if (isFarSide(axis, p.targetAnchor) || p.targetAnchor == WidgetOrientation.Center) {
//                        yield(Dependency(p.relativeTo, DependencyKind.Dimensions, axis))
//                    }
//                    if (isFarSide(axis, p.selfAnchor) || p.selfAnchor == WidgetOrientation.Center) {
//                        yield(Dependency(w, DependencyKind.Dimensions, axis))
//                    }
//                }
//                is WidgetPosition.Absolute -> {
//                    if (isFarSide(axis, p.anchorTo) || p.anchorTo == WidgetOrientation.Center) {
//                        yield(Dependency(w, DependencyKind.Dimensions, axis))
//                    }
//                }
//            }
//
//            if (axis == Axis.X || axis == Axis.Y) {
//                val d = w.dimensions[axis]
//                when (d) {
//                    is WidgetDimensions.Proportional -> yield(Dependency(parent, DependencyKind.Dimensions, axis))
//                    is WidgetDimensions.Relative -> yield(Dependency(parent, DependencyKind.Dimensions, axis))
//                    is WidgetDimensions.ExpandToParent -> {
//                        yield(Dependency(parent, DependencyKind.Dimensions, axis))
//                        yield(Dependency(w, DependencyKind.Dimensions, axis))
//                    }
//                    is WidgetDimensions.Intrinsic -> {
//                        if (w.dimensions[oppositeAxis(axis)] !is WidgetDimensions.Intrinsic) {
//                            yield(Dependency(w, DependencyKind.Dimensions, oppositeAxis(axis)))
//                        }
//                    }
//                    is WidgetDimensions.WrapContent -> {
//                        for (c in w.children) {
//                            yield(Dependency(c, DependencyKind.PartialPosition, axis))
//                            yield(Dependency(c, DependencyKind.Dimensions, axis))
//                        }
//                    }
//                    is WidgetDimensions.ExpandTo -> {
//                        yield(Dependency(d.expandTo, DependencyKind.Position, axis))
//                        yield(Dependency(d.expandTo, DependencyKind.Dimensions, axis))
//                    }
//                    is WidgetDimensions.Fixed -> {}
//                }
//            }
//        }
//    }
//}