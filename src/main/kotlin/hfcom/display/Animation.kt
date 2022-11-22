package hfcom.display

import arx.core.*
import arx.display.core.ImageRef
import arx.display.core.mix
import arx.engine.*
import arx.engine.Event
import hfcom.game.*
import org.lwjgl.glfw.GLFW
import java.lang.Float.max
import java.lang.Float.min
import java.util.Collections.emptyIterator
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.*
import kotlin.reflect.KClass

private var AnimationIdCounter = AtomicLong(0L)

enum class AnimationKind {
    CharacterMove,
    AP,
    Damage,
    Color,
    Delay,
    Death,
    AttackResult
}

sealed class Animation(val entity: Entity?) {
    var id: Long = AnimationIdCounter.incrementAndGet()
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

    fun withDuration(d: Float): Animation {
        duration = d
        return this
    }

    fun withNext(anim: Animation): Animation {
        followedBy = followedBy + anim
        return this
    }

    /**
     * Returns the position at which to draw this animation, if it has its own
     * drawing apart from simply modifying the drawing of existing entities
     */
    fun drawPosition(): MapCoordf? {
        return null
    }

    abstract val animationKind: AnimationKind

}

interface PositionAnimation {
    fun currentPosition() : MapCoordf
}


data class AnimationSprite(val position : MapCoordf ,val image : ImageRef, val dimensions : Vec2f, val color : RGBA = White)

interface SpriteAnimation {
    fun sprites() : List<AnimationSprite>
}


data class CharacterMoveAnimation(val character: Entity, val from: MapCoord, val to: MapCoord) : Animation(character), PositionAnimation {
    override fun currentPosition(): MapCoordf {
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


data class DamageAnimation(val character: Entity, val fromHPLost: Int, val toHPLost: Int) : Animation(character) {
    override val animationKind = AnimationKind.Damage

    fun currentHPLost(): Int {
        return (fromHPLost.toFloat() + (toHPLost - fromHPLost).toFloat() * f).roundToInt()
    }
}

data class MissAnimation(val character: Entity, val position : MapCoord, val dodge: Boolean) : Animation(character), PositionAnimation, SpriteAnimation {
    override val animationKind: AnimationKind = AnimationKind.CharacterMove

    val left = ThreadLocalRandom.current().nextBoolean()
    override fun currentPosition(): MapCoordf {
        val mf = if (f < 0.5) { f } else { 1.0f - f }

        val pos = position.toMapCoordf()

        val delta = if (left) { MapCoordf(-0.4f, 0.0f, 0.0f) } else { MapCoordf(0.4f, 0.0f, 0.0f) }

        return pos + delta * mf + MapCoordf(0.0f, 0.0f, sin(mf * 2.0f * 3.14159f) * 0.15f)
    }

    override fun sprites(): List<AnimationSprite> {
        val missImg = Resources.image("display/ui/miss.png").toImage()
        val dimensions = Vec2f(0.5f, 0.5f / missImg.aspectRatio)
        val color = RGBAf(1.0f,1.0f,1.0f,cos(f * PI.toFloat() * 0.5f))
        return listOf(AnimationSprite(position = position.toMapCoordf() + MapCoordf(0.0f,0.0f,1.75f + f), image = missImg, dimensions = dimensions, color = color))
    }
}

data class TintAnimation(val character: Entity, val tintColor: RGBA) : Animation(character) {
    override val animationKind = AnimationKind.Color

    fun currentColor(): RGBA {
        val cosf = (cos(f * PI * 2.0f) + 1.0f) * 0.5f
        return mix(RGBA(255u, 255u, 255u, 255u), tintColor, cosf.toFloat())
    }
}

data class DelayAnimation(val dur: Float) : Animation(null) {
    init {
        duration = dur
    }

    override val animationKind = AnimationKind.Delay
}

data class CharacterAPAnimation(val character: Entity, val from: Double, val to: Double) : Animation(character) {

    fun currentAP(): Double {
        return from + (to - from) * f
    }

    override val animationKind = AnimationKind.AP
}

data class CharacterDeathAnimation(val character : Entity, val position : MapCoord) : Animation(character), PositionAnimation {
    override val animationKind = AnimationKind.Death

    // this is a position animation since it involves drawing this entity at a position it may no longer
    // occupy... since dead entities don't occupy any position
    override fun currentPosition(): MapCoordf {
        return position.toMapCoordf()
    }
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
    val animationsByEntityAndKind: Map<Entity, Map<AnimationKind, Animation>>,
    val entityAnimatedPositions: Map<Entity, MapCoordf>,
    val animatedEntitiesByPosition: Map<MapCoord, List<Pair<Entity, MapCoordf>>>,
    val animatedEntitiesByPosition2D: Map<MapCoord2D, List<Pair<Entity, MapCoordf>>>,
) {
    fun animationsForEntity(ent: Entity): Iterator<Animation> {
        return animationsByEntityAndKind[ent]?.values?.iterator() ?: emptyIterator()
    }

    fun nonEmpty(): Boolean {
        return animationsByEntityAndKind.isNotEmpty()
    }

    fun hasPositionAnimation(e: Entity): Boolean {
        return entityAnimatedPositions.contains(e)
    }

    fun positionAnimatedEntitiesAt(c: MapCoord): List<Pair<Entity, MapCoordf>> {
        return animatedEntitiesByPosition[c] ?: emptyList()
    }

    fun positionAnimatedEntitiesAt(c: MapCoord2D): List<Pair<Entity, MapCoordf>> {
        return animatedEntitiesByPosition2D[c] ?: emptyList()
    }

    fun animatedPositionFor(e : Entity): MapCoordf? {
        return entityAnimatedPositions[e]
    }

    fun animations(): Iterator<Animation> {
        return iterator {
            for (m in animationsByEntityAndKind.values) {
                for ((_,v) in m) {
                    yield(v)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> animationsForEntityAndType(e: Entity, c: KClass<T>): T? {
        for (anim in animationsForEntity(e)) {
            if (c.isInstance(anim)) {
                return anim as T?
            }
        }
        return null
    }
}

data class AnimationData(
    var animationGroups: List<AnimationGroup> = listOf()
) : DisplayData, CreateOnAccessData {
    companion object : DataType<AnimationData>(AnimationData(), sparse = true)

    override fun dataType(): DataType<*> {
        return AnimationData
    }

    val emptyAnimationGroup = AnimationGroup(emptyList())

    private val emptyAnimationContext: AnimationContext = AnimationContext(mapOf(), mapOf(), mapOf(), mapOf())
    private var animationContextIntern: AnimationContext = emptyAnimationContext
    val animationContext: AnimationContext
        get() {
            return animationContextIntern
        }


    fun updateAnimationContext(animationsByEntityAndKind: Map<Entity, Map<AnimationKind, Animation>>, pos: Map<Entity, MapCoordf>) {
        val inverted = mutableMapOf<MapCoord, List<Pair<Entity, MapCoordf>>>()
        val inverted2D = mutableMapOf<MapCoord2D, List<Pair<Entity, MapCoordf>>>()
        for ((e, c) in pos) {
            inverted[c.toMapCoord(round = false)] = (inverted[c.toMapCoord(round = false)] ?: emptyList()) + Pair(e, c)
            inverted2D[c.toMapCoord2D(round = false)] = (inverted2D[c.toMapCoord2D(round = false)] ?: emptyList()) + Pair(e, c)
        }
        animationContextIntern = AnimationContext(animationsByEntityAndKind, pos, inverted, inverted2D)
    }

    fun clearAnimationContext() {
        animationContextIntern = emptyAnimationContext
    }

    fun createAnimationGroup(vararg animations: Animation) {
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
        val AD = world[AnimationData]

        return if (AD.animationGroups.isNotEmpty()) {
            val now = GLFW.glfwGetTime().toFloat()

            var rerun = true
            var updateContext = false

            while (rerun && AD.animationGroups.isNotEmpty()) {
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

            val organizedAnims: Map<Entity, Map<AnimationKind, Animation>> = if (updateContext) {
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
                            Noto.warn("We don't really handle non-entity animations yet : $anim")
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
                        is PositionAnimation -> {
                            anim.entity.expectLet {
                                positionOverrides = positionOverrides + (it to anim.currentPosition())
                            }
                        }
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

    override fun handleEvent(world: World, event: Event) {
        with(world) {
            if (event !is GameEvent) {
                return
            }

            val AD = world[AnimationData]

            when (event) {
                is CharacterMoved -> AD.createAnimationGroup(
                    CharacterMoveAnimation(event.character, event.from, event.to).withDuration(
                        event.from.xy.distanceTo(event.to.xy) * 0.25f + min(
                            abs(event.from.z - event.to.z).toFloat(),
                            1.0f
                        ) * 0.1f
                    )
                )
                is DamageTaken -> AD.createAnimationGroup(
                    DelayAnimation(0.15f).withNext(
                        TintAnimation(event.character, RGBA(255u, 0u, 0u, 255u)).withDuration(0.3f).withNext(
                            DamageAnimation(event.character, event.hpLostBefore, event.hpLostAfter).withDuration((event.hpLostAfter - event.hpLostBefore).toFloat() * 0.2f)
                        )
                    )
                )
                is CharacterDied -> AD.createAnimationGroup(
                    CharacterDeathAnimation(event.character, (+event.character[Physical]).position).withDuration(0.5f)
                )
                is AttackMiss -> AD.createAnimationGroup(
                    MissAnimation(event.target, position = event.target[Physical]!!.position,dodge = true).withDuration(0.75f)
                )
            }
        }
    }
}