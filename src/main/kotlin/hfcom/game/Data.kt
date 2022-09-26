package hfcom.game

import arx.core.*
import arx.core.Taxonomy.UnknownThing
import arx.display.core.ImagePath
import arx.display.core.ImageRef
import arx.display.core.SentinelImage
import arx.engine.DataType
import arx.engine.Entity
import arx.engine.GameData
import arx.game.core.SimpleLibrary
import com.typesafe.config.ConfigValue
import hfcom.display.MapCoord


data class CombatStats (
    var strength : Int = 0,
    var speed : Int = 0,
    var attack : Int = 0,
    var defence : Int = 0,
    var encumbrance : Int = 0,
    var protection : Int = 0
) : GameData {
    companion object : DataType<CombatStats>( CombatStats() )
    override fun dataType() : DataType<*> { return CombatStats }
}

operator fun CombatStats?.unaryPlus() : CombatStats {
    return this?: CombatStats.defaultInstance
}


data class CharacterData (
    var hp : Reduceable = Reduceable(10),
    var characterClass : Taxon = UnknownThing
) : GameData {
    companion object : DataType<CharacterData>(CharacterData())
    override fun dataType() : DataType<*> { return CharacterData }
}

operator fun CharacterData?.unaryPlus() : CharacterData {
    return this?: CharacterData.defaultInstance
}



data class CharacterClass (
    var image : ImageRef = SentinelImage,
) : FromConfig {
    override fun readFromConfig(cv: ConfigValue) {
        image = ImageRef.createFromConfig(cv["image"])
    }
}

object CharacterClasses : SimpleLibrary<CharacterClass>("CharacterClasses", listOf("hfcom/data/CharacterClasses.sml"), { CharacterClass() })



data class Physical (
    var position : MapCoord = MapCoord(0,0,0),
    var size : Int = 2
) : GameData {
    companion object : DataType<Physical>( Physical() )
    override fun dataType() : DataType<*> { return Physical }
}

operator fun Physical?.unaryPlus() : Physical {
    return this?: Physical.defaultInstance
}




data class TerrainType(
    var images: List<ImageRef> = listOf(ImagePath("hfcom/display/terrain/stone.png"))
) : FromConfig {
    override fun readFromConfig(cv: ConfigValue) {
        images = cv["images"].asList().map { ImageRef.createFromConfig(it) }
    }
}


object Terrains : SimpleLibrary<TerrainType>("Terrains", listOf("hfcom/data/Terrains.sml"), { TerrainType() })




data class Tile(
    var terrains : List<Taxon> = emptyList(),
    var entities : List<Entity> = emptyList()
)

data class TacticalMap (
    var tiles : FiniteGrid2D<Tile> = FiniteGrid2D(Vec2i(128,128), Tile()),
    var groundLevel : Int = 2, // purely a display convenience, represents the baseline expected depth of terrain

) : GameData {
    companion object : DataType<TacticalMap>( TacticalMap() )
    override fun dataType() : DataType<*> { return TacticalMap }
}

operator fun TacticalMap?.unaryPlus() : TacticalMap {
    return this?: TacticalMap.defaultInstance
}
