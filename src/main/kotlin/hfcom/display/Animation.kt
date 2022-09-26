package hfcom.display

import arx.core.Vec3f
import arx.engine.*
import hfcom.game.CharacterMoved
import org.lwjgl.glfw.GLFW
import java.lang.Float.max
import java.lang.Float.min
import java.util.concurrent.atomic.AtomicLong

private var AnimationIdCounter = AtomicLong(0L)

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

}


data class CharacterMoveAnimation(val character: Entity, val from: MapCoord, val to: MapCoord) : Animation(character) {
    fun currentPosition() : MapCoordf {
        val delta = (to - from).toFloat()
        val raw = from.toFloat() + delta * f
        return MapCoordf(raw)
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

data class AnimationData(
    var animationGroups: List<AnimationGroup> = listOf()
) : DisplayData {
    companion object : DataType<AnimationData>(AnimationData(), sparse = true)

    override fun dataType(): DataType<*> {
        return AnimationData
    }

    val emptyAnimationGroup = AnimationGroup(emptyList())

    private var entityAnimatedPositions : Map<Entity, MapCoordf> = mapOf()
    private var animatedEntitiesByPosition : Map<MapCoord2D, List<Pair<Entity, MapCoordf>>> = mapOf()


    fun hasPositionAnimation(e : Entity) : Boolean {
        return entityAnimatedPositions.contains(e)
    }
    fun positionAnimatedEntitiesAt(c: MapCoord2D) : List<Pair<Entity, MapCoordf>> {
        return animatedEntitiesByPosition[c] ?: emptyList()
    }

    fun setEntityPositionAnimations(pos : Map<Entity, MapCoordf>) {
        entityAnimatedPositions = pos
        val inverted = mutableMapOf<MapCoord2D, List<Pair<Entity, MapCoordf>>>()
        for ((e,c) in pos) {
            inverted[c.toMapCoord2D(round = false)] = (inverted[c.toMapCoord2D(round = false)] ?: emptyList()) + Pair(e, c)
        }
        animatedEntitiesByPosition = inverted
    }


    val activeAnimationGroup: AnimationGroup
        get() {
            return animationGroups.getOrNull(0) ?: emptyAnimationGroup
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
            val ag = AD.animationGroups[0]
            var finished : List<Animation> = emptyList()
            for (anim in ag.animations) {
                if (anim.startedAt == null) {
                    anim.startedAt = now
                    fireEvent(AnimationStarted(anim))
                }
                anim.curTime = now

                if (anim.curTime!! - anim.startedAt!! >= anim.duration) {
                    fireEvent(AnimationEnded(anim))
                    finished = finished + anim
                }
            }

            var toAdd : List<Animation> = emptyList()
            for (anim in finished) {
                toAdd = toAdd + anim.followedBy
            }
            if (finished.isNotEmpty()) {
                ag.animations = ag.animations - finished.toSet()
            }
            ag.animations = ag.animations + toAdd
            if (ag.animations.isEmpty()) {
                AD.animationGroups = AD.animationGroups.drop(1)
            }


            var positionOverrides = mapOf<Entity, MapCoordf>()
            for (anim in AD.activeAnimationGroup.animations) {
                when (anim) {
                    is CharacterMoveAnimation -> positionOverrides = positionOverrides + (anim.character to anim.currentPosition())
                }
            }
            AD.setEntityPositionAnimations(positionOverrides)

            true
        } else {
            AD.setEntityPositionAnimations(mapOf())

            false
        }
    }

    override fun handleEvent(world: World, e: Event) {
        if (e !is GameEvent) { return }

        when (e) {
            is CharacterMoved -> world[AnimationData]?.createAnimationGroup(CharacterMoveAnimation(e.character, e.from, e.to).withDuration(e.from.distanceTo(e.to) * 0.25f))
        }
    }
}