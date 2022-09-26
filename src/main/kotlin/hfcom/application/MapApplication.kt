package hfcom.application

import arx.application.Application
import arx.core.*
import arx.display.components.CameraComponent
import arx.display.components.CameraID
import arx.display.core.PixelCamera
import arx.engine.*
import hfcom.display.*
import hfcom.game.*


object MainCamera : CameraID {
    override val startingCamera = PixelCamera().apply { origin = Vec3f(0.0f, 0.0f, 0.0f); scaleIncrement = 32.0f; speed = Vec3f(8.0f, 8.0f, 8.0f); scale = 5.0f }
}

object MapGeneratorComponent : GameComponent() {
    override fun initialize(world: GameWorld) {
        world.attachData(TacticalMap())

        with(world) {
            val tm = world.global(TacticalMap)!!

            for (x in 0 until 64) {
                for (y in 0 until 64) {
                    val entities = if (x == 2 && y == 2) {
                        val c = createEntity()
                        c.attachData(Physical(position = MapCoord(x,y,2), size = 2))
                        c.attachData(CharacterData(hp = Reduceable(10), characterClass = t("CharacterClasses.Archer")))
                        listOf(c)
                    } else {
                        emptyList()
                    }

                    val terrains = if (x == 5 && y == 2) {
                        listOf(t("Terrains.Stone"),t("Terrains.Stone"),t("Terrains.Grass"))
                    } else if (x == 6 && y == 2) {
                        listOf(t("Terrains.Stone"),t("Terrains.Stone"), t("Terrains.Stone"), t("Terrains.Grass"))
                    } else {
                        listOf(t("Terrains.Stone"), t("Terrains.Grass"))
                    }
                    tm.tiles[x, y] = Tile(terrains, entities = entities)
                }
            }
        }
    }

    override fun update(world: GameWorld) {
        // do nothing
    }

    override fun handleEvent(world: GameWorld, event: GameEvent) {
        // do nothing
    }
}



fun main() {
    Application()
        .run(
            Engine(
                mutableListOf(MapGeneratorComponent),
                mutableListOf(AnimationComponent, CameraComponent, TacticalMapComponent)
            )
        )
}