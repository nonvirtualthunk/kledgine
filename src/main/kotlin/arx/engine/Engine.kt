package arx.engine


abstract class EngineComponent {
    var engine : Engine? = null

    fun fireEvent(event : Event) {
        engine!!.handleEvent(event)
    }
}


enum class EventPriority {
    Last,
    VeryLow,
    Low,
    Normal,
    High,
    VeryHigh,
    First
}

abstract class DisplayComponent : EngineComponent() {
    var eventPriority = EventPriority.Normal

    open fun initialize(world: World) {}

    open fun update(world: World) : Boolean { return false }

    open fun draw(world: World) {}

    open fun handleEvent(world: World, event: Event) {}
}


class ModificationContext(val modificationsByDataType: Array<MutableSet<EntityId>>, var keyFrameSet : Boolean, var keyFrameDuration : Float) {
    fun <T>modified(dt: DataType<T>, e : Entity) {
        modificationsByDataType[dt.index].add(e.id)
    }

    /**
     * Indicate that the point we are currently at is a key frame that should be animated between,
     * also indicates the overall duration that that transition ought to take
     */
    fun makeKeyFrame(duration: Float) {
        keyFrameSet = true
        keyFrameDuration = duration
    }
}

interface AnimatingDisplayComponent {
    fun markAffected(world: World, event: GameEvent, ctx: ModificationContext)

    fun updateData(entity: Entity, dataType: DataType<*>)
}

abstract class GameComponent : EngineComponent() {
    abstract fun initialize(world: GameWorld)

    abstract fun update(world: GameWorld)

    abstract fun handleEvent(world: GameWorld, event: GameEvent)
}




class Engine(
    var gameComponents : MutableList<GameComponent> = mutableListOf(),
    var displayComponents : MutableList<DisplayComponent> = mutableListOf()
) {

    var world = World()

    val components : List<EngineComponent> get() { return gameComponents + displayComponents }

    init {
        components.forEach { it.engine = this }
    }

    fun initialize() {
        for (gc in gameComponents) {
            gc.initialize(world)
        }
        for (dc in displayComponents) {
            dc.initialize(world)
        }
    }

    fun updateGameState() {
        for (gc in gameComponents) {
            gc.update(world)
        }
    }

    fun updateDisplayState() : Boolean {
        var anyNeedsRedraw = false
        for (dc in displayComponents) {
            if (dc.update(world)) {
                anyNeedsRedraw = true
            }
        }
        return anyNeedsRedraw
    }

    fun draw() {
        for (dc in displayComponents) {
            dc.draw(world)
        }
    }

    fun handleEvent(event: Event) {
        if (event is GameEvent) {
            for (gc in gameComponents) {
                gc.handleEvent(world, event)
            }
        }

        for (dc in displayComponents) {
            dc.handleEvent(world, event)
        }
    }
}