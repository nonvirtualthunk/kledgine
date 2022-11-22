package hfcom.game

import arx.core.*
import arx.core.Taxonomy.UnknownThing
import arx.display.core.ImageRef
import arx.engine.*
import arx.game.core.SimpleLibrary
import com.typesafe.config.ConfigValue
import hfcom.display.MapCoord
import kotlin.math.roundToInt


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
    val visionRange: Int = 0,              // how far the entity can see
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
        maxHP = cv["maxHP"].asInt() ?: 0,
        visionRange = cv["visionRange"].asInt() ?: 0,
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
            visionRange + other.visionRange,
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
    companion object : DataType<FlagData>(FlagData()), FromConfigCreator<FlagData> {
        override fun createFromConfig(cv: ConfigValue?): FlagData? {
            if (cv == null) { return null }
            return FlagData().apply { readFromConfig(cv) }
        }
    }

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

data class StatsChange(val stats: CombatStats, val endCondition : Taxon, val source: String)


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
    var activeActionIdentifier: ActionIdentifier? = null,
    var statsChanges: List<StatsChange> = emptyList(),
    var dead : Boolean = false
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

    data class Move(override val source: Entity) : ActionIdentifier
}


sealed interface Effect {
    fun defaultTarget(): TargetKind

    fun isValidTarget(world: GameWorld, actor: Entity, target: EffectTarget): Boolean

    data class AttackEffect(val attack: Attack) : Effect {
        override fun defaultTarget(): TargetKind {
            return TargetKind.Enemy
        }

        override fun isValidTarget(world: GameWorld, actor: Entity, target: EffectTarget): Boolean {
            with(world) {
                when (target) {
                    is EffectTarget.Entity -> return isEnemy(actor, target.entity) && attackSummary(actor, target, attack) != null
                    else -> return false
                }
            }
        }
    }

    data class StatsEffect(val stats: CombatStats, val duration: Taxon) : Effect {
        override fun defaultTarget(): TargetKind {
            return TargetKind.Self
        }

        override fun isValidTarget(world: GameWorld, actor: Entity, target: EffectTarget): Boolean {
            return when (target) {
                is EffectTarget.Entity -> true
                else -> false
            }
        }
    }

    object Move : Effect {
        override fun defaultTarget(): TargetKind {
            return TargetKind.Tile
        }

        override fun isValidTarget(world: GameWorld, actor: Entity, target: EffectTarget): Boolean {
            with(world) {
                return when (target) {
                    is EffectTarget.Tile -> {
                        actor[Physical]?.let { pd -> pathfinder(world, actor).findPath(pd.position, Pathfinder.SingleDestination(target.at), target.at) } != null
                    }
                    else -> false
                }
            }
        }

        override fun toString(): String {
            return "Move"
        }
    }

    object NoOp : Effect {
        override fun defaultTarget(): TargetKind {
            return TargetKind.Self
        }

        override fun isValidTarget(world: GameWorld, actor: Entity, target: EffectTarget): Boolean {
            return false
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


interface TargetPredicate {
    fun GameWorld.isValidTarget(actor: Entity, target : EffectTarget) : Boolean

    companion object : FromConfigCreator<TargetPredicate> {
        override fun createFromConfig(cv: ConfigValue?): TargetPredicate? {
            return TargetKind.createFromConfig(cv)
        }
    }
}


enum class TargetKind : TargetPredicate {
    Self,
    Enemy,
    Tile;

    companion object : FromConfigCreator<TargetKind> {
        override fun createFromConfig(cv: ConfigValue?): TargetKind? {
            if (cv == null) {
                return null
            }
            return when (cv.asStr()?.lowercase()) {
                "self" -> Self
                "enemy" -> Enemy
                "tile" -> Tile
                else -> null
            }
        }
    }

    override fun GameWorld.isValidTarget(actor: Entity, target : EffectTarget) : Boolean {
        return when(this@TargetKind) {
            Self -> target == EffectTarget.Entity(actor)
            Enemy -> (target as? EffectTarget.Entity)?.let { t -> isEnemy(actor, t.entity) } ?: false
            Tile -> target is EffectTarget.Tile
        }
    }
}




sealed interface EffectTarget {
    data class Entity(val entity: arx.engine.Entity) : EffectTarget

    data class Tile(val at: MapCoord) : EffectTarget

    data class Path(val path : Pathfinder.Path<MapCoord>) : EffectTarget
}

data class TargetedEffect(val effect: Effect, val targetingRules: List<TargetPredicate>) {
    companion object : FromConfigCreator<TargetedEffect> {
        override fun createFromConfig(cv: ConfigValue?): TargetedEffect? {
            val eff = Effect.createFromConfig(cv) ?: return null
            var targetingRules = (cv["target"].asList() + cv["targets"].asList()).mapNotNull { TargetPredicate.createFromConfig(it) }
            if (targetingRules.isEmpty()) {
                targetingRules = listOf(eff.defaultTarget())
            }
            return TargetedEffect(eff, targetingRules)
        }
    }

    fun isValidTarget(world: GameWorld, actor: Entity, target: EffectTarget): Boolean {
        return effect.isValidTarget(world, actor, target)
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
val MoveAction = Action("Move", listOf(TargetedEffect(Effect.Move, listOf(TargetKind.Tile))), 0)

data class Skill(
    val name: String,
    val action: Action,
    val icon: ImageRef? = null
) {
    constructor (cv: ConfigValue) : this(
        name = cv["name"].asStr() ?: "Unnamed Skill",
        action = Action(cv),
        icon = cv["icon"]?.let { ImageRef(it) }
    )
}

object Skills : SimpleLibrary<Skill>("Skills", listOf("hfcom/data/Skills.sml"), { Skill(it) })


fun GameWorld.effectiveCombatStats(ent: Entity): CombatStats {
    return effectiveCombatStatsContributions(ent).merge()
}


fun GameWorld.effectiveCombatStatsContributions(ent: Entity): List<CombatStatsContribution> {
    val cd = ent[CharacterData]
    return if (cd == null) {
        Noto.err("effectiveCombatStats(...) is only expected to be called on a character")
        emptyList()
    } else {
        val ret = mutableListOf(CombatStatsContribution(cd.combatStats, "base character stats"))
        for (cl in cd.classLevels) {
            ret.add(CombatStatsContribution(cl.combatStats, cl.name))
        }
        for ((equipped, _) in cd.equippedItems) {
            equipped[Item]?.equippedCombatStats?.let {
                ret.add(CombatStatsContribution(it, equipped[Identity]?.nameOrKind() ?: "Item"))
            }
        }
        for (sc in cd.statsChanges) {
            ret.add(CombatStatsContribution(sc.stats, sc.source))
        }
        ret
    }
}


fun List<CombatStatsContribution>.merge() : CombatStats {
    return this.fold(CombatStats()) { a,b -> a + b.stats }
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
    val flagData : FlagData,
    override var identity: Taxon
) : Identifiable {
    companion object : FromConfigCreator<ItemType> {
        override fun createFromConfig(cv: ConfigValue?): ItemType? {
            if (cv == null) {
                return null
            }
            return ItemType(
                itemData = Item().apply { readFromConfig(cv) },
                weaponData = cv["attacks"]?.let { Weapon(cv) },
                flagData = FlagData(cv) ?: FlagData(),
                identity = UnknownThing,
            )
        }
    }
}

fun GameWorld.createItem(itemType: ItemType, implicitItem: Boolean = false): Entity {
    val ent = createEntity()
    ent.attachData(itemType.itemData.copy(implicitItem = implicitItem))
    ent.attachData(Identity(identity = itemType.identity))
    ent.attachData(itemType.flagData)
    itemType.weaponData?.let { ent.attachData(it.copy()) }
    return ent
}

object ItemTypes : SimpleLibrary<ItemType>("Items", listOf("hfcom/data/Items.sml"), { ItemType.createFromConfig(it) })



data class Object (
    val passable : Boolean = false,
    val opaque : Boolean = true,
    val providesSupport : Boolean = true,       // i.e. can you stand on it
    val display : ObjectDisplay = ObjectDisplay.Default
) : GameData {
    companion object : DataType<Object>( Object() ), FromConfigCreator<Object> {
        override fun createFromConfig(cv: ConfigValue?): Object? {
            if (cv == null) { return null }
            return Object(
                passable = cv["passable"].asBool() ?: false,
                display = ObjectDisplay(cv["display"]) ?: ObjectDisplay.Default
            )
        }
    }
    override fun dataType() : DataType<*> { return Object }
}


sealed interface ObjectDisplay {
    data class Simple(val image : ImageRef, val color : RGBA = White) : ObjectDisplay
    data class Wall(val pillar : ImageRef, val swToNe : ImageRef, val seToNw : ImageRef, val color : RGBA = White) : ObjectDisplay


    companion object : FromConfigCreator<ObjectDisplay> {
        val Default = Simple(ImageRef("hfcom/display/objects/book_1.png"))
        override fun createFromConfig(cv: ConfigValue?): ObjectDisplay? {
            return if (cv.isStr()) {
                Simple(image = ImageRef.createFromConfig(cv), color = White)
            } else if (cv.isObject() && cv["pillar"] != null) {
                Wall(pillar = ImageRef.createFromConfig(cv["pillar"]), seToNw = ImageRef.createFromConfig(cv["seToNw"]), swToNe = ImageRef.createFromConfig(cv["swToNe"]), color = White)
            } else {
                null
            }
        }
    }
}

data class ObjectType (
    val objectData : Object,
    val physicalData : Physical,
) : Identifiable {
    override var identity: Taxon = UnknownThing
    companion object : FromConfigCreator<ObjectType> {
        override fun createFromConfig(cv: ConfigValue?): ObjectType? {
            if (cv == null) { return null }
            return ObjectType(
                objectData = Object(cv) ?: Object(),
                physicalData = Physical(cv) ?: Physical()
            )
        }
    }
}

object ObjectTypes : SimpleLibrary<ObjectType>("Objects", listOf("hfcom/data/Objects.sml"), { ObjectType(it) })


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

data class CombatStatsContribution(val stats : CombatStats, val from : String)

data class AttackSummary(
    val attack : Attack,
    val hitFraction : Float,
    val minDamage : Int,
    val maxDamage : Int,
    val range : Int,
    val attackerStatsContributions : List<CombatStatsContribution>,
    val defenderStatsContributions : List<CombatStatsContribution>,
) {
    val hitPercentDisplay : String get() { return (hitFraction * 100.0).roundToInt().toString() }
    val damageRangeDisplay : String get() {
        return if (minDamage != maxDamage) {
            "$minDamage - $maxDamage"
        } else {
            "$minDamage"
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
    companion object : DataType<Physical>(Physical()), FromConfigCreator<Physical> {
        override fun createFromConfig(cv: ConfigValue?): Physical? {
            if (cv == null) { return null }
            return Physical(
                position = MapCoord(0, 0, 0),
                size = cv["size"].asInt() ?: 2
            )
        }
    }

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


class Tile(terr : List<Taxon?> = emptyList()) {

    var terrains: List<Taxon?> = terr
        set(v) {
            field = v
            recalculateBitsets()
        }

    var entities: Set<Entity> = emptySet()
        private set

    private var occupiedByEntities = BitVec32()
    private var supportByEntities = BitVec32()
    private var opaqueByEntities = BitVec32()

    var occupied: BitVec32 = BitVec32()
        private set
    var support: BitVec32 = BitVec32()
        private set
    var opaque: BitVec32 = BitVec32()
        private set
    var firstUnoccupiedLevel: Byte = 0
        private set
    var lastOccupiedLevel: Byte = 0
        private set
    var firstSupportLevel: Byte = 0
        private set
    var lastSupportLevel: Byte = 0
        private set

    init {
        recalculateBitsets()
    }

    fun addEntity(world : GameWorld, e : Entity) {
        if (! entities.contains(e)) {
            entities = entities + e
            recalculateEntityBitsets(world)
        }
    }

    fun removeEntity(world : GameWorld, e : Entity) {
        entities = entities - e
        recalculateEntityBitsets(world)
    }

    private fun recalculateEntityBitsets(world: GameWorld) {
        occupiedByEntities = BitVec32()
        supportByEntities = BitVec32()
        opaqueByEntities = BitVec32()
        with(world) {
            for (ent in this@Tile.entities) {
                val pd = ent[Physical] ?: continue
                val od = ent[Object]

                val isOpaque = od?.opaque ?: false
                val isSolid = !(od?.passable ?: false)
                val isSupport = od?.providesSupport ?: false

                for (j in 0 until pd.size) {
                    if (isSolid) {
                        occupiedByEntities = occupiedByEntities.setBit(pd.position.z + j)
                    }
                    if (isOpaque) {
                        opaqueByEntities = opaqueByEntities.setBit(pd.position.z + j)
                    }
                    if (isSupport) {
                        supportByEntities = supportByEntities.setBit(pd.position.z + j)
                    }
                }
            }
        }
        recalculateBitsets()
    }

    private fun recalculateBitsets() {
        var occ = occupiedByEntities
        var opa = opaqueByEntities
        var sup = supportByEntities

        for (i in 0 until terrains.size) {
            if (terrains[i] != null) {
                occ = occ.setBit(i)
                opa = opa.setBit(i)
                sup = sup.setBit(i)
            }
        }

        occupied = occ
        opaque = opa
        support = sup

        firstUnoccupiedLevel = 32
        firstSupportLevel = 32
        lastSupportLevel = -1
        for (i in 0 until 31) {
            val o = occupied[i]
            val s = support[i]
            if (! o && firstUnoccupiedLevel == 32.toByte()) {
                firstUnoccupiedLevel = i.toByte()
            }
            if (o) {
                lastOccupiedLevel = i.toByte()
            }
            if (s && firstSupportLevel == 32.toByte()) {
                firstSupportLevel = i.toByte()
            }
            if (s) {
                lastSupportLevel = i.toByte()
            }
        }
    }

    /**
     * Iterates through the z levels on this tile that could be occupied
     * by a character of the given height. That is, all z levels that have
     * at least [height] units of open space from a terrain perspective.
     * And solid terrain immediately under. I.e. two stacked terrains at
     * the base level would return a single value of `2`
     */
    fun occupiableZLevels(height: Int): Iterator<Int> {
        return iterator {
            var i = firstUnoccupiedLevel - 1
            while (i <= lastOccupiedLevel) {
                if (support[i]) {
                    var ok = true
                    for (j in 1 .. height) {
                        if (occupied[i + j]) {
                            ok = false
                            break
                        }
                    }
                    if (ok) {
                        yield(i+1)
                        i += height - 1
                    }
                }
                i += 1
            }
        }
    }

    /**
     * Iterates through the z levels on this tile that are supported. Effectively
     * that indicates the places that something could be placed, whether or not
     * something else has already been placed there. More exactly, this is all
     * z where [z - 1] is a support but [z] is not
     */
    fun supportedZLevels(): Iterator<Int> {
        return iterator {
            var i = firstSupportLevel.toInt()
            while (i <= lastSupportLevel.toInt()) {
                if (support[i] && ! support[i + 1]) {
                    yield(i+1)
                }
                i += 1
            }
        }
    }
}

data class TacticalMap(
    var tiles: FiniteGrid2D<Tile> = FiniteGrid2D(Vec2i(128, 128), Tile()),
    var groundLevel: Int = 2, // purely a display convenience, represents the baseline expected depth of terrain
    var activeFaction: Taxon = t("Factions.Player"),
    var entities: MutableSet<Entity> = mutableSetOf(),

) : GameData {
    companion object : DataType<TacticalMap>(TacticalMap())

    override fun dataType(): DataType<*> {
        return TacticalMap
    }

}

operator fun TacticalMap?.unaryPlus(): TacticalMap {
    return this ?: TacticalMap.defaultInstance
}
