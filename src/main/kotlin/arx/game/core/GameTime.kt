package arx.game.core

import arx.engine.*
import java.util.concurrent.atomic.AtomicLong

class GameTime : GameData, CreateOnAccessData {
    companion object : DataType<GameTime>( GameTime() )
    override fun dataType() : DataType<*> { return GameTime }

    internal var gameTicksInternal : AtomicLong = AtomicLong(0)
    val gameTicks : Long get() { return gameTicksInternal.get() }
    internal var gameEventsInternal : AtomicLong = AtomicLong(0)
    val gameEvents : Long get() { return gameEventsInternal.get() }
}

object GameTimeComponent : GameComponent() {
    override fun initialize(world: GameWorld) {
        // do nothing
    }

    override fun update(world: GameWorld) {
        world[GameTime].gameTicksInternal.incrementAndGet()
    }

    override fun handleEvent(world: GameWorld, event: GameEvent) {
        world[GameTime].gameEventsInternal.incrementAndGet()
    }
}