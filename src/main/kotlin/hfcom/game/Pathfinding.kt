package hfcom.game

import arx.core.Pathfinder
import arx.core.Taxonomy.UnknownThing
import arx.engine.Entity
import arx.engine.GameWorld
import arx.engine.World
import hfcom.display.MapCoord
import kotlin.math.abs


private val dxs = arrayOf(-1, 1, 0, 0)
private val dys = arrayOf(0, 0, -1, 1)



fun pathfinder(world: GameWorld, character: Entity) : Pathfinder<MapCoord> {
    val tm = world[TacticalMap]!!
    val cd = +world.data(character, CharacterData)
    val pd = +world.data(character, Physical)

    return Pathfinder(
        { c, out ->
//            with(world) {
//                val charFaction = cd.faction
                val charSize = pd.size
                for (q in 0 until 4) {
                    val ax = c.x + dxs[q]
                    val ay = c.y + dys[q]
                    if (ax >= 0 && ay >= 0 && ax < tm.tiles.dimensions.x && ay < tm.tiles.dimensions.y) {
                        val tile = tm.tiles[ax, ay]
                        for (z in tile.occupiableZLevels(charSize)) {
                            if (c.z - z <= 3 && z - c.z <= 2) {
                                out.add(MapCoord(ax, ay, z))
                            }
                        }
                    }
                }
//            }
        },
        { from, to ->
            world.moveCost(tm, cd, from, to)
        },
        { from, target ->
            val ax = abs(from.x - target.x)
            val ay = abs(from.y - target.y)
            val raw = (ax + ay + abs(from.z - target.z)).toDouble()
            if (ax > ay) {
                raw + ax * 0.001
            } else {
                raw + ay * 0.001
            }
        }
    )
}