package hfcom.display

import arx.core.Noto
import arx.engine.*
import hfcom.game.CharacterMoved
import org.lwjgl.glfw.GLFW
import java.lang.Float.max
import java.lang.Float.min
import java.util.Collections.emptyIterator
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.sin

private var AnimationIdCounter = AtomicLong(0L)

enum class AnimationKind {
    CharacterMove,
    AP
}

sealed class Animation(val entity: Entity?) {
    var id : Long = AnimationIdCounter.incrementAndGet()
    var startedAt: Float? = null
    var curTime: Float? = null
    var duration = 1.0f
    var followedBy: List<Animation> = listOf()

    val f: Float
        get() {
            return if (startedAt == null || curTime == null) {
                0.0f
            } else {
                min((curTime!! - startedAt!!) / max(duration, 0.0001f), 1.0f)
            }
        }

    fun withDuration(d : Float) : Animation {
        duration = d
        return this
    }

    /**
     * Returns the position at which to draw this animation, if it has its own
     * drawing apart from simply modifying the drawing of existing entities
     */
    fun drawPosition() : MapCoordf? {
        return null
    }

    abstract val animationKind : AnimationKind

}


data class CharacterMoveAnimation(val character: Entity, val from: MapCoord, val to: MapCoord) : Animation(character) {
    fun currentPosition() : MapCoordf {
        val delta = (to - from).toFloat()
        val raw = from.toFloat() + delta * f

        return if (to.z > from.z) {
            val sn = sin(f * 2.5f)
            val pz = if (f < 0.5f) {
                sn * 1.25f
            } else {
                max(sn * 1.25f, 1.0f)
            }
            raw.z = from.z + (pz * (to.z - from.z).toFloat())
            MapCoordf(raw)
        } else if (to.z < from.z) {
            val pz = max(sin((f * 2.3f + 0.93f)) * 1.25f, 0.0f)
            raw.z = to.z + (from.z - to.z) * pz
            MapCoordf(raw)
        } else {
            MapCoordf(raw)
        }
    }

    override val animationKind = AnimationKind.CharacterMove
}

data class CharacterAPAnimation(val character : Entity, val from : Double, val to : Double) : Animation(character) {

    fun currentAP() : Double {
        return from + (to - from) * f
    }

    override val animationKind = AnimationKind.AP
}


data class AnimationGroup(var animations: List<Animation>) {
    fun animationsFor(e: Entity): List<Animation> {
        return animations.filter { it.entity == e }
    }

    fun animationsByEntity(): Map<Entity, List<Animation>> {
        return if (animations.isNotEmpty()) {
            val ret = mutableMapOf<Entity, List<Animation>>()
            animations.forEach { a ->
                if (a.entity != null) {
                    ret[a.entity] = (ret[a.entity] ?: emptyList()) + a
                }
            }
            ret
        } else {
            emptyMap()
        }
    }
}

data class AnimationContext(
    val animationsByEntityAndKind : Map<Entity, Map<AnimationKind, Animation>>,
    val entityAnimatedPositions : Map<Entity, MapCoordf>,
    val animatedEntitiesByPosition : Map<MapCoord2D, List<Pair<Entity, MapCoordf>>>,
) {
    fun animationsForEntity(ent : Entity) : Iterator<Animation> {
        return animationsByEntityAndKind[ent]?.values?.iterator() ?: emptyIterator()
    }

    fun nonEmpty() : Boolean {
        return animationsByEntityAndKind.isNotEmpty()
    }

    fun hasPositionAnimation(e : Entity) : Boolean {
        return entityAnimatedPositions.contains(e)
    }
    fun positionAnimatedEntitiesAt(c: MapCoord2D) : List<Pair<Entity, MapCoordf>> {
        return animatedEntitiesByPosition[c] ?: emptyList()
    }
}

data class AnimationData(
    var animationGroups: List<AnimationGroup> = listOf()
) : DisplayData {
    companion object : DataType<AnimationData>(AnimationData(), sparse = true)

    override fun dataType(): DataType<*> {
        return AnimationData
    }

    val emptyAnimationGroup = AnimationGroup(emptyList())

    private val emptyAnimationContext : AnimationContext = AnimationContext(mapOf(), mapOf(), mapOf())
    private var animationContextIntern : AnimationContext = emptyAnimationContext
    val animationContext : AnimationContext get() { return animationContextIntern }


    fun updateAnimationContext(animationsByEntityAndKind : Map<Entity, Map<AnimationKind, Animation>>, pos : Map<Entity, MapCoordf>) {
        val inverted = mutableMapOf<MapCoord2D, List<Pair<Entity, MapCoordf>>>()
        for ((e,c) in pos) {
            inverted[c.toMapCoord2D(round = false)] = (inverted[c.toMapCoord2D(round = false)] ?: emptyList()) + Pair(e, c)
        }
        animationContextIntern = AnimationContext(animationsByEntityAndKind, pos, inverted)
    }

    fun clearAnimationContext() {
        animationContextIntern = emptyAnimationContext
    }

    fun createAnimationGroup(vararg animations : Animation) {
        animationGroups = animationGroups + AnimationGroup(animations.toList())
    }
}

data class AnimationStarted(val animation: Animation) : DisplayEvent()
data class AnimationEnded(val animation: Animation) : DisplayEvent()

operator fun AnimationData?.unaryPlus(): AnimationData {
    return this ?: AnimationData.defaultInstance
}


object AnimationComponent : DisplayComponent() {
    override fun initialize(world: World) {
        world.register(AnimationData)
        world.attachData(AnimationData())
    }

    override fun update(world: World): Boolean {
        val AD = world[AnimationData] ?: return false

        return if (AD.animationGroups.isNotEmpty()) {
            val now = GLFW.glfwGetTime().toFloat()

            var rerun = true
            var updateContext = false

            while(rerun && AD.animationGroups.isNotEmpty()) {
                rerun = false

                val ag = AD.animationGroups[0]
                var finished: List<Animation> = emptyList()
                for (anim in ag.animations) {
                    if (anim.startedAt == null) {
                        anim.startedAt = now
                        fireEvent(AnimationStarted(anim))
                        rerun = true
                        updateContext = true
                    }
                    anim.curTime = now

                    if (anim.curTime!! - anim.startedAt!! >= anim.duration) {
                        fireEvent(AnimationEnded(anim))
                        finished = finished + anim
                        rerun = true
                        updateContext = true
                    }
                }

                var toAdd: List<Animation> = emptyList()
                for (anim in finished) {
                    toAdd = toAdd + anim.followedBy
                }
                if (finished.isNotEmpty()) {
                    ag.animations = ag.animations - finished.toSet()
                }
                ag.animations = ag.animations + toAdd
                if (ag.animations.isEmpty()) {
                    AD.animationGroups = AD.animationGroups.drop(1)
                    rerun = true
                }
            }

            val organizedAnims : Map<Entity, Map<AnimationKind, Animation>> = if (updateContext) {
                val ret = mutableMapOf<Entity, MutableMap<AnimationKind, Animation>>()
                for (ag in AD.animationGroups) {
                    // do a breadth first iteration through animations, keeping the first one for any
                    // given (entity, kind) combination
                    val q = ArrayDeque<Animation>()
                    for (an in ag.animations) {
                        q.addLast(an)
                    }
                    while (q.isNotEmpty()) {
                        val anim = q.removeFirst()
                        if (anim.entity != null) {
                            val m = ret.getOrPut(anim.entity) { mutableMapOf() }
                            m.putIfAbsent(anim.animationKind, anim)
                        } else {
                            Noto.warn("We don't really handle non-entity animations yet")
                        }
                        for (childAnim in anim.followedBy) {
                            q.addLast(childAnim)
                        }
                    }
                }
                ret
            } else {
                AD.animationContext.animationsByEntityAndKind
            }

            var positionOverrides = mapOf<Entity, MapCoordf>()
            for (animMap in organizedAnims.values) {
                for (anim in animMap.values) {
                    when (anim) {
                        is CharacterMoveAnimation -> positionOverrides = positionOverrides + (anim.character to anim.currentPosition())
                        else -> {}
                    }
                }
            }
            AD.updateAnimationContext(organizedAnims, positionOverrides)

            true
        } else {
            AD.clearAnimationContext()

            false
        }
    }

    override fun handleEvent(world: World, e: Event) {
        if (e !is GameEvent) { return }

        when (e) {
            is CharacterMoved -> world[AnimationData]?.createAnimationGroup(CharacterMoveAnimation(e.character, e.from, e.to).withDuration(e.from.xy.distanceTo(e.to.xy) * 0.25f + min(abs(e.from.z - e.to.z).toFloat(), 1.0f) * 0.1f))
        }
    }
}