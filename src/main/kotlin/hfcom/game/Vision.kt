package hfcom.game

import arx.core.RPAS3dShadowcaster
import arx.core.ShadowGrid
import arx.core.Shadowcaster
import arx.core.Vec3i
import arx.engine.*
import hfcom.display.MapCoord
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong


data class VisionKey (val viewer : Entity, val position : MapCoord)

data class CharacterVision (val visibility : ShadowGrid, val position: MapCoord, val visionRange: Int, val visionRevision : Long)

data class Vision (
    val visionCache : ConcurrentHashMap<VisionKey, Future<CharacterVision>> = ConcurrentHashMap(),
    val keysOfInterest : MutableSet<VisionKey> = mutableSetOf(), // keys we want to actively keep updated
    var visionRevision : AtomicLong = AtomicLong(0L)
) : GameData, CreateOnAccessData {
    companion object : DataType<Vision>( Vision() )
    override fun dataType() : DataType<*> { return Vision }
}


fun GameWorld.characterVisionAt(character : Entity, at : MapCoord) : CharacterVision {
    val v = this[Vision]
    val vkey = VisionKey(character, at)
    val fut = v.visionCache.getOrPut(vkey) {
        CompletableFuture.completedFuture(computeVision(vkey, ShadowGrid()))
    }

    val charVis = fut.get()
    if (charVis.visionRevision != v.visionRevision.get()) {
        v.visionCache[vkey] = CompletableFuture.completedFuture(computeVision(vkey, charVis.visibility))
    }
    return charVis
}


internal fun GameWorld.computeVision(visionKey : VisionKey, out : ShadowGrid?) : CharacterVision {
    val tm = this[TacticalMap]!!
    val vis = this[Vision]

    val tdim = tm.tiles.dimensions
    VisionComponent.shadowcaster = RPAS3dShadowcaster({ x, y, z ->
        if (tm.tiles[x, y].opaque[z]) {
            1.0f
        } else {
            0.0f
        }
    }, { x, y, z ->
        x >= 0 && x < tdim.x && y >= 0 && y < tdim.y
    }, Vec3i(2, 2, 1))

    val grid = out ?: ShadowGrid()

    val visionRange = effectiveCombatStats(visionKey.viewer).visionRange
    VisionComponent.shadowcaster.shadowcast(grid, visionKey.position, visionRange * 2)

    return CharacterVision(visibility = grid, visionRevision = vis.visionRevision.get(), position = visionKey.position, visionRange = visionRange)
}


object VisionComponent : GameComponent() {
    lateinit var shadowcaster : Shadowcaster

    val executor: ExecutorService = Executors.newFixedThreadPool(4)

    override fun initialize(world: GameWorld) {

    }

    override fun update(world: GameWorld) {

    }

    override fun handleEvent(world: GameWorld, event: GameEvent) {
        with(world) {
            when (event) {
                is VisibilityChange -> {
                    val vis = this[Vision]
                    vis.visionRevision.incrementAndGet()
                    vis.keysOfInterest.forEach { key ->
                        val existing = vis.visionCache[key]
                        val grid = existing?.get()?.visibility
                        vis.visionCache[key] = executor.submit<CharacterVision> { computeVision(key, grid) }
                    }
                }
                is SetVisionKeysOfInterest -> {
                    val vis = this[Vision]
                    vis.keysOfInterest.removeIf { it.viewer == event.viewer }
                    vis.keysOfInterest.addAll(event.keys)
                    for (key in event.keys) {
                        val existing = vis.visionCache[key]
                        if (existing == null) {
                            vis.visionCache[key] = executor.submit<CharacterVision> { computeVision(key, ShadowGrid()) }
                        } else if (existing.isDone && existing.get().visionRevision != vis.visionRevision.get()) {
                            vis.visionCache[key] = executor.submit<CharacterVision> { computeVision(key, existing.get().visibility) }
                        }
                    }
                }
                is EntityPlaced -> {
                    if (event.entity[Object]?.opaque == true) {
                        world.fireEvent(VisibilityChange(event.entity[Physical]?.position ?: MapCoord(0,0,0)))
                    }
                }
                else -> {}
            }
        }
    }
}


data class SetVisionKeysOfInterest(val viewer : Entity, val keys : List<VisionKey>) : GameEvent()