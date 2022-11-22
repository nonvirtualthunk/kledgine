package hfcom.application

import arx.application.Application
import arx.core.*
import arx.display.components.CameraComponent
import arx.display.components.CameraID
import arx.display.core.PixelCamera
import arx.display.windowing.WindowingSystemComponent
import arx.engine.*
import arx.game.core.GameTimeComponent
import hfcom.display.*
import hfcom.game.*
import io.github.config4k.toConfig


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
                    val terrains = if (x == 5 && y == 2) {
                        listOf(t("Terrains.Stone"),t("Terrains.Stone"),t("Terrains.Grass"))
                    } else if (x == 6 && y == 2) {
                        listOf(t("Terrains.Stone"),t("Terrains.Stone"), t("Terrains.Stone"), t("Terrains.Grass"))
                    } else if (x == 7 && y == 2) {
                        listOf(t("Terrains.Stone"),t("Terrains.Stone"), t("Terrains.Stone"), t("Terrains.Stone"), t("Terrains.Grass"))
                    } else {
                        listOf(t("Terrains.Stone"), t("Terrains.Grass"))
                    }
                    tm.tiles[x, y] = Tile(terrains)


                    val entities = if (x == 2 && y == 2) {
                        listOf(createCharacter(t("CharacterClasses.Archer"), t("Factions.Player"), "Tobold"))
                    } else if (x == 4 && y == 3) {
                        listOf(createCharacter(t("CharacterClasses.Warden"), t("Factions.Player"), "Jolgan"))
                    } else if (x == 2 && y == 6) {
                        listOf(createCharacter(t("CharacterClasses.Cultist"), t("Factions.Enemy"), "Cultist"))
                    } else if (x == 3 && y == 8) {
                        listOf(createCharacter(t("CharacterClasses.Cultist"), t("Factions.Enemy"), "Cultist"))
                    } else if (x == 5 && y == 5) {
                        listOf(createObject(ObjectTypes[t("Objects.Wall")]!!))
                    } else {
                        emptyList()
                    }

                    for (c in entities) {
                        placeEntity(c, MapCoord2D(x, y))
                    }
                }
            }

            for (ent in entitiesWithData(CharacterData)) {
                startMission(ent)

                startTurn(ent)

                println(possibleActions(ent))
            }

            for (ent in entities) {
                if (ent[hfcom.game.CharacterData] != null || ent[Object] != null) {
                    ent.printAllData()
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
        .apply {
            clearColor = RGBAf(0.0f,0.0f,0.0f,1.0f)
        }
        .run(
            Engine(
                mutableListOf(MapGeneratorComponent, GameTimeComponent, VisionComponent),
                mutableListOf(AnimationComponent, CameraComponent, TacticalMapComponent, WindowingSystemComponent)
            )
        )
}