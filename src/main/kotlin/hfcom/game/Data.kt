package hfcom.game

import arx.core.*
import arx.core.Taxonomy.UnknownThing
import arx.display.core.ImageRef
import arx.engine.*
import arx.game.core.SimpleLibrary
import com.typesafe.config.ConfigValue
import hfcom.display.MapCoord


/**
 * All stats that are merged together as part of combat from various sources. I.e.
 * a character has innate stats, contributions from equipment, class levels, etc.
 * This is a bit less about a coherent collection of values and more about how
 * they are used at present.
 */
data class CombatStats(
    val strength: Int = 0,                 // raw damage dealt on hit
    val speed: Int = 0,                    // actions per turn
    val accuracy: Int = 0,                 // melee accuracy
    val precision: Int = 0,                // ranged accuracy
    val defence: Int = 0,                  // dodge, damage avoidance
    val encumbrance: Int = 0,              // weight, difficulty to use, adds to fatigue
    val protection: Int = 0,               // physical damage reduction
    val maxHP: Int = 0,                    // maximum hp a character can have
    val flags: Map<Taxon, Int> = mapOf(),  // general purpose flags
) {
    constructor(cv: ConfigValue?) : this(
        strength = cv["strength"].asInt() ?: 0,
        speed = cv["speed"].asInt() ?: 0,
        accuracy = cv["accuracy"].asInt() ?: 0,
        precision = cv["precision"].asInt() ?: 0,
        defence = cv["defence"].asInt() ?: 0,
        encumbrance = cv["encumbrance"].asInt() ?: 0,
        protection = cv["protection"].asInt() ?: 0,
        flags = cv["flags"].map { k, v ->
            if (v.isBool()) {
                t(k) to if (v.asBool() == true) {
                    1
                } else {
                    0
                }
            } else {
                v.asInt()?.expectLet {
                    t(k) to it
                }
            }
        }
    )

    operator fun plus(other: CombatStats): CombatStats {

        return CombatStats(
            strength + other.strength,
            speed + other.speed,
            accuracy + other.accuracy,
            precision + other.precision,
            defence + other.defence,
            encumbrance + other.encumbrance,
            protection + other.protection,
            maxHP + other.maxHP,
            mergeFlags(flags, other.flags)
        )
    }

    companion object : FromConfigCreator<CombatStats> {
        override fun createFromConfig(cv: ConfigValue?): CombatStats? {
            return CombatStats(cv)
        }
    }

//    override fun readFromConfig(cv: ConfigValue) {
//        cv["strength"].asInt()?.let { strength = it }
//        cv["speed"].asInt()?.let { speed = it }
//        cv["accuracy"].asInt()?.let { accuracy = it }
//        cv["precision"].asInt()?.let { precision = it }
//        cv["defence"].asInt()?.let { defence = it }
//        cv["encumbrance"].asInt()?.let { encumbrance = it }
//        cv["protection"].asInt()?.let { protection = it }
//        cv["maxHP"].asInt()?.let { maxHP = it }
//        for ((k,v) in cv["flags"]) {
//            if (v.isBool()) {
//                flags = flags + (t(k) to if (v.asBool() == true) { 1 } else { 0 })
//            } else {
//                v.asInt()?.expectLet {
//                    flags = flags + (t(k) to it)
//                }
//            }
//        }
//    }
}


data class Flag(
    val name: String,
    val countDown: Int?,
    val countUp: Int?,
    val multiplyBy: Int?,
    val divideBy: Int?,
    val clearOn: Taxon?
) {
    companion object : FromConfigCreator<Flag> {
        override fun createFromConfig(cv: ConfigValue?): Flag? {
            if (cv == null) {
                return null
            }
            return Flag(
                name = cv["name"].asStr() ?: "Unknown Flag Name",
                countDown = cv["countDown"].asInt(),
                countUp = cv["countUp"].asInt(),
                multiplyBy = cv["multiplyBy"].asInt(),
                divideBy = cv["divideBy"].asInt(),
                clearOn = cv["clearOn"].asStr()?.toTaxon()
            )
        }
    }
}

object Flags : SimpleLibrary<Flag>("Flags", listOf("hfcom/data/Flags.sml"), { Flag.createFromConfig(it) })


data class FlagData(
    var flags: Map<Taxon, Int> = mapOf()
) : GameData, FromConfig {
    companion object : DataType<FlagData>(FlagData())

    override fun dataType(): DataType<*> {
        return FlagData
    }

    override fun readFromConfig(cv: ConfigValue) {
        for ((k, v) in cv["flags"]) {
            if (v.isBool()) {
                flags = flags + (t(k) to if (v.asBool() == true) {
                    1
                } else {
                    0
                })
            } else {
                v.asInt()?.expectLet {
                    flags = flags + (t(k) to it)
                }
            }
        }
    }

    operator fun plus(other: FlagData): FlagData {
        return FlagData(flags = mergeFlags(this.flags, other.flags))
    }
}

fun mergeFlags(a: Map<Taxon, Int>, b: Map<Taxon, Int>): Map<Taxon, Int> {
    var mergedFlags = a
    for ((k, v) in b) {
        mergedFlags = mergedFlags + (k to mergedFlags.getOrDefault(k, 0) + v)
    }
    return mergedFlags
}

operator fun FlagData?.unaryPlus(): FlagData {
    return this ?: FlagData.defaultInstance
}


data class CharacterData(
    var hpLost: Int = 0,
    var ap: Double = 0.0,
    var fatigue: Int = 0,
    var characterClass: Taxon = UnknownThing,
    var combatStats: CombatStats = CombatStats(),
    var itemSlots: List<Taxon> = listOf(),
    var equippedItems: Map<Entity, List<Taxon>> = mapOf(),
    var faction: Taxon = t("Factions.Enemy"),
    var classLevels: List<CharacterClassLevel> = listOf(),
    var activeAction: ActionIdentifier? = null,
) : GameData {
    companion object : DataType<CharacterData>(CharacterData())

    override fun dataType(): DataType<*> {
        return CharacterData
    }

    fun unoccupiedSlots(): List<Taxon> {
        val copy = mutableListOf<Taxon>()
        copy.addAll(itemSlots)
        for ((_, vs) in equippedItems) {
            for (v in vs) {
                copy.remove(v)
            }
        }
        return copy
    }

    fun occupiedSlots(): List<Taxon> {
        val ret = mutableListOf<Taxon>()
        for ((_, vs) in equippedItems) {
            for (v in vs) {
                ret.add(v)
            }
        }

        return ret
    }

}

fun GameWorld.equipItem(cd: CharacterData, item: Entity): Boolean {
    val itemData = item[Item] ?: return false

    var slots = cd.unoccupiedSlots()
    for (targetSlot in itemData.slots) {
        if (slots.contains(targetSlot)) {
            slots = slots - targetSlot
        } else {
            return false
        }
    }
    cd.equippedItems = cd.equippedItems + (item to itemData.slots)
    return true
}

operator fun CharacterData?.unaryPlus(): CharacterData {
    return this ?: CharacterData.defaultInstance
}


sealed interface ActionIdentifier {
    val source: Entity

    data class Skill(override val source: Entity, val kind: Taxon) : ActionIdentifier
    data class Attack(override val source: Entity, val name: String) : ActionIdentifier
    data class CastSpell(override val source: Entity, val spell: Taxon) : ActionIdentifier
}


sealed interface Effect {
    fun defaultTarget(): TargetKind
    data class AttackEffect(val attack: Attack) : Effect {
        override fun defaultTarget(): TargetKind {
            return TargetKind.Enemy
        }
    }

    data class StatsEffect(val stats: CombatStats, val duration: Taxon) : Effect {
        override fun defaultTarget(): TargetKind {
            return TargetKind.Self
        }
    }

    object NoOp : Effect {
        override fun defaultTarget(): TargetKind {
            return TargetKind.Self
        }
    }

    companion object : FromConfigCreator<Effect> {
        override fun createFromConfig(cv: ConfigValue?): Effect? {
            val stats = cv["stats"]
            if (stats != null) {
                return StatsEffect(CombatStats(cv), stats["duration"].asTaxon())
            }
            val attackCV = cv["attack"]
            if (attackCV != null) {
                return Attack(attackCV).expectLet {
                    AttackEffect(it)
                }
            }
            return null
        }
    }
}

enum class TargetKind {
    Self,
    Enemy;

    companion object : FromConfigCreator<TargetKind> {
        override fun createFromConfig(cv: ConfigValue?): TargetKind? {
            if (cv == null) {
                return null
            }
            return when (cv.asStr()?.lowercase()) {
                "self" -> Self
                "enemy" -> Enemy
                else -> null
            }
        }
    }
}

data class TargetedEffect(val effect: Effect, val target: TargetKind) {
    companion object : FromConfigCreator<TargetedEffect> {
        override fun createFromConfig(cv: ConfigValue?): TargetedEffect? {
            val eff = Effect.createFromConfig(cv) ?: return null
            val target = TargetKind.createFromConfig(cv["target"]) ?: eff.defaultTarget()
            return TargetedEffect(eff, target)
        }
    }
}

data class Action(
    val name: String,
    val effects: List<TargetedEffect>,
    val ap: Int,
) {
    constructor(cv: ConfigValue) : this(
        name = cv["name"].asStr() ?: "Unnamed Action",
        effects = cv["effects"].asList().mapNotNull { TargetedEffect(it) },
        ap = cv["ap"].asInt() ?: 5
    )
}

val NoOpAction = Action("No Action", listOf(), 1)

data class Skill(
    val name: String,
    val action: Action
) {
    constructor (cv: ConfigValue) : this(
        name = cv["name"].asStr() ?: "Unnamed Skill",
        action = Action(cv)
    )
}

object Skills : SimpleLibrary<Skill>("Skills", listOf("hfcom/data/Skills.sml"), { Skill(it) })


fun GameWorld.effectiveCombatStats(ent: Entity): CombatStats {
    val cd = ent[CharacterData]
    return if (cd == null) {
        Noto.err("effectiveCombatStats(...) is only expected to be called on a character")
        CombatStats()
    } else {
        var ret = cd.combatStats
        for ((equipped, _) in cd.equippedItems) {
            equipped[Item]?.equippedCombatStats?.let {
                ret += it
            }
        }
        for (cl in cd.classLevels) {
            ret += cl.combatStats
        }
        ret
    }
}

fun GameWorld.effectiveFlags(ent: Entity): Map<Taxon, Int> {
    val cd = ent[CharacterData]
    val fd = ent[FlagData]
    return if (cd == null) {
        fd?.flags ?: mapOf()
    } else {
        val cs = effectiveCombatStats(ent)
        mergeFlags((fd?.flags ?: mapOf()), cs.flags)
    }
}

data class Item(
    var slots: List<Taxon> = listOf(t("ItemSlots.Head")),
    var equippedCombatStats: CombatStats = CombatStats(),
    var skills: List<Taxon> = emptyList(),
    var implicitItem: Boolean = false, // indicates this is default equipment for a class, or similar
) : GameData, FromConfig {
    companion object : DataType<Item>(Item())

    override fun dataType(): DataType<*> {
        return Item
    }

    override fun readFromConfig(cv: ConfigValue) {
        slots = cv["slots"].asTaxonList()
        equippedCombatStats = CombatStats(cv["equippedCombatStats"] ?: cv["combatStats"] ?: cv)
        skills = cv["skills"].asTaxonList()
    }
}

operator fun Item?.unaryPlus(): Item {
    return this ?: Item.defaultInstance
}


data class ItemType(
    val itemData: Item,
    val weaponData: Weapon?,
    override var identity : Taxon
) : Identifiable {
    companion object : FromConfigCreator<ItemType> {
        override fun createFromConfig(cv: ConfigValue?): ItemType? {
            if (cv == null) {
                return null
            }
            return ItemType(
                itemData = Item().apply { readFromConfig(cv) },
                weaponData = cv["attacks"]?.let { Weapon(cv) },
                identity = UnknownThing
            )
        }
    }
}

fun GameWorld.createItem(itemType : ItemType, implicitItem: Boolean = false): Entity {
    val ent = createEntity()
    ent.attachData(itemType.itemData.copy(implicitItem = implicitItem))
    ent.attachData(Identity(identity = itemType.identity))
    itemType.weaponData?.let { ent.attachData(it.copy()) }
    return ent
}

object ItemTypes : SimpleLibrary<ItemType>("Items", listOf("hfcom/data/Items.sml"), { ItemType.createFromConfig(it) })


data class Attack(
    val name: String = "Attack",
    val accuracy: Int = 0,
    val range: Int = 0,
    val ammunition: Int = 0,
    val damage: Int = 1,
    val times: Int = 1,
    var ap: Int = 5
) {
    companion object : FromConfigCreator<Attack> {
        override fun createFromConfig(cv: ConfigValue?): Attack {
            return Attack(
                name = cv["name"].asStr() ?: "Attack",
                accuracy = cv["accuracy"].asInt() ?: 0,
                range = cv["range"].asInt() ?: 0,
                ammunition = cv["ammunition"].asInt() ?: 0,
                damage = cv["damage"].asInt() ?: 0,
                times = cv["times"].asInt() ?: 1
            )
        }
    }
}

data class Weapon(
    var attacks: List<Attack> = listOf(),
    var ammunition: Reduceable = Reduceable(0)
) : GameData, FromConfig {
    constructor (cv: ConfigValue?) : this() {
        readFromConfig(cv)
    }

    companion object : DataType<Weapon>(Weapon())

    override fun dataType(): DataType<*> {
        return Weapon
    }

    override fun readFromConfig(cv: ConfigValue) {
        attacks = cv["attacks"].asList().map { Attack.createFromConfig(it) }
        ammunition = Reduceable(cv["amunition"].asInt() ?: 0)
    }
}

operator fun Weapon?.unaryPlus(): Weapon {
    return this ?: Weapon.defaultInstance
}


data class CharacterClass(
    val image: ImageRef,
    val levels: List<List<CharacterClassOption>>
) {
    companion object : FromConfigCreator<CharacterClass> {
        override fun createFromConfig(cv: ConfigValue?): CharacterClass? {
            return CharacterClass(
                image = ImageRef.createFromConfig(cv["image"]),
                levels = cv["levels"].asList().map {
                    it.asList().map { clocv -> CharacterClassOption(clocv) }
                }
            )
        }
    }

    fun level0(): CharacterClassLevel? {
        if (levels.isNotEmpty()) {
            if (levels[0].isNotEmpty()) {
                return levels[0][0].classLevel
            }
        }
        return null
    }
}


data class CharacterClassOption(
    var classLevel: CharacterClassLevel = CharacterClassLevel()
) : FromConfig {
    constructor (cv: ConfigValue?) : this() {
        readFromConfig(cv)
    }

    override fun readFromConfig(cv: ConfigValue) {
        classLevel = CharacterClassLevel(cv["classLevel"] ?: cv)
    }
}

data class CharacterClassLevel(
    var combatStats: CombatStats = CombatStats(),
    var image: ImageRef? = null,
    var name: String = "Level Up",
    var equipment: List<Taxon> = listOf(),
    var skills: List<Taxon> = listOf(),
) {
    constructor(cv: ConfigValue) : this(
        combatStats = CombatStats(cv["combatStats"] ?: cv),
        image = cv["image"]?.let { ImageRef.createFromConfig(it) },
        name = cv["name"].asStr() ?: "Class Level",
        equipment = cv["equipment"].asTaxonList(),
        skills = cv["skills"].asTaxonList()
    )

}

object CharacterClasses : SimpleLibrary<CharacterClass>("CharacterClasses", listOf("hfcom/data/CharacterClasses.sml"), { CharacterClass.createFromConfig(it) })

fun main() {
    for ((k, v) in CharacterClasses) {
        println("$k :\n\t$v")
        println("----------------------------------")
    }
}


data class Physical(
    var position: MapCoord = MapCoord(0, 0, 0),
    var size: Int = 2
) : GameData {
    companion object : DataType<Physical>(Physical())

    override fun dataType(): DataType<*> {
        return Physical
    }
}

operator fun Physical?.unaryPlus(): Physical {
    return this ?: Physical.defaultInstance
}


data class TerrainType(
    val images: List<ImageRef>,
    val moveCost: Float
) {
    companion object : FromConfigCreator<TerrainType> {
        override fun createFromConfig(cv: ConfigValue?): TerrainType? {
            return TerrainType(
                images = cv["images"].asList().map { ImageRef.createFromConfig(it) },
                moveCost = cv["moveCost"].asFloat() ?: 1.0f
            )
        }
    }
}


object Terrains : SimpleLibrary<TerrainType>("Terrains", listOf("hfcom/data/Terrains.sml"), { TerrainType.createFromConfig(it) })


data class Tile(
    var terrains: List<Taxon?> = emptyList(),
    var entities: List<Entity> = emptyList()
) {
    /**
     * Iterates through the z levels on this tile that could be occupied
     * by a character of the given height. That is, all z levels that have
     * at least [height] units of open space from a terrain perspective.
     * And solid terrain immediately under. I.e. two stacked terrains at
     * the base level would return a single value of `2`
     */
    fun occupiableZLevels(height: Int): Iterator<Int> {
        return iterator {
            yield(terrains.size)
        }
    }
}

data class TacticalMap(
    var tiles: FiniteGrid2D<Tile> = FiniteGrid2D(Vec2i(128, 128), Tile()),
    var groundLevel: Int = 2, // purely a display convenience, represents the baseline expected depth of terrain

) : GameData {
    companion object : DataType<TacticalMap>(TacticalMap())

    override fun dataType(): DataType<*> {
        return TacticalMap
    }

}

operator fun TacticalMap?.unaryPlus(): TacticalMap {
    return this ?: TacticalMap.defaultInstance
}
