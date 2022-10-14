package hfcom.game

import arx.core.*
import arx.engine.Entity
import arx.engine.GameWorld
import arx.engine.prettyString
import hfcom.display.CharacterDeathAnimation
import hfcom.display.MapCoord
import hfcom.display.MapCoord2D
import kotlin.math.max


fun GameWorld.createCharacter(cclass: Taxon, faction: Taxon, name: String?): Entity {
    val ccd = CharacterClasses[cclass]

    val c = createEntity()
    c.attachData(Physical(size = 2))
    c.attachData(CharacterData(
        characterClass = cclass,
        combatStats = CombatStats(maxHP = 2),
        faction = faction,
        classLevels = ccd?.level0().ifLet { listOf(it) }.orElse { emptyList() },
        itemSlots = listOf(t("Head"), t("Hand"), t("Hand"), t("Belt"), t("Chest"), t("Legs"), t("Misc"), t("Misc"))
    )
    )
    c.attachData(Identity(t("Character"), name))
    return c
}


fun GameWorld.placeEntity(ent: Entity, at: MapCoord2D) {
    val tm = global(TacticalMap) ?: return

    val pd = ent[Physical] ?: return

    val tile = tm.tiles[at]
    val z = tile.occupiableZLevels(pd.size).next()
    tile.entities = tile.entities + ent
    pd.position = MapCoord(at, z)

    fireEvent(CharacterPlaced(ent, pd.position))
}

fun GameWorld.moveCost(map: TacticalMap, cd: CharacterData, from: MapCoord, to: MapCoord): Double {
    val baseCost = map.tiles[to.x, to.y].terrains.getOrNull(to.z - 1)?.let { terrain ->
        Terrains[terrain]?.moveCost?.toDouble()
    } ?: 1.0

    val zMultiplier = if (to.z < from.z) {
        0.5
    } else {
        (to.z - from.z + 1).toDouble()
    }

    return baseCost * zMultiplier
}

fun GameWorld.isEnemy(a: Entity, b: Entity): Boolean {
    val acd = a[CharacterData] ?: return false
    val bcd = b[CharacterData] ?: return false
    return acd.faction != bcd.faction
}

fun GameWorld.useAP(ent: Entity, cost: Double): Boolean {
    val cd = ent[CharacterData] ?: return Noto.errAndReturn("Cannot consume ap from non-character entity ${ent.toString(this)}", false)
    return if (cd.ap < cost - 0.000001) {
        false
    } else {
        cd.ap = max(cd.ap - cost, 0.0)
        fireEvent(CharacterUsedAP(ent, cost, cd.ap))
        true
    }
}

fun GameWorld.moveCharacter(ent: Entity, path: Pathfinder.Path<MapCoord>) : Boolean {
    val tm = global(TacticalMap) ?: return false
    val pd = ent[Physical] ?: return false
    val cd = ent[CharacterData] ?: return false

    for (step in path.steps.drop(1)) {
        val cost = moveCost(tm, cd, pd.position, step)
        if (!useAP(ent, cost)) {
            return false
        }

        val tile = tm.tiles[pd.position.x, pd.position.y]
        tile.entities = tile.entities - ent

        val newTile = tm.tiles[step.x, step.y]
        newTile.entities = newTile.entities + ent
        val oldPos = pd.position
        pd.position = MapCoord(step.x, step.y, step.z)

        fireEvent(CharacterMoved(ent, oldPos, pd.position))
    }
    return true
}

fun GameWorld.startMission(ent: Entity) {
    val cd = ent[CharacterData] ?: return

    for (cl in cd.classLevels.reversed()) {
        for (eq in cl.equipment) {
            ItemTypes[eq]?.let {
                // create a default item to equip if the character has not
                // equipped something explicitly in the relevant slots
                val createdEquipment = createItem(it, implicitItem = true)
                // if we can't equip the item (because something else is occupying
                // the slots it would take), then destroy the temporary item we made
                if (!equipItem(cd, createdEquipment)) {
                    destroyEntity(createdEquipment)
                }
            }
        }
    }

}

fun GameWorld.endMission(ent: Entity) {
    val cd = ent[CharacterData] ?: return

    cd.hpLost = 0
}

fun GameWorld.startTurn(ent: Entity) {
    val cd = ent[CharacterData] ?: return

    val cs = effectiveCombatStats(ent)
    cd.ap = cs.speed.toDouble() + 10.0


}


fun GameWorld.possibleAttacks(ent: Entity): Map<ActionIdentifier, Attack> {
    val cd = ent[CharacterData] ?: return emptyMap()
    val ret = mutableMapOf<ActionIdentifier, Attack>()
    for ((item, _) in cd.equippedItems) {
        val wd = item[Weapon] ?: continue
        for (attack in wd.attacks) {
            ret[ActionIdentifier.Attack(item, attack.name)] = attack
        }
    }
    return ret
}

fun GameWorld.possibleActions(ent: Entity): Map<ActionIdentifier, Action> {
    val cd = ent[CharacterData] ?: return emptyMap()

    val ret = mutableMapOf<ActionIdentifier, Action>()
    for ((k, a) in possibleAttacks(ent)) {
        ret[k] = Action(name = a.name, effects = listOf(TargetedEffect(Effect.AttackEffect(a), TargetKind.Enemy)), ap = a.ap)
    }

    val classSkills = cd.classLevels.flatMap { it.skills }
    for (skill in classSkills) {
        Skills[skill].expectLet {
            ret[ActionIdentifier.Skill(ent, skill)] = it.action
        }
    }
    cd.equippedItems.keys.forEach { item ->
        item[Item]?.skills?.forEach { skill ->
            Skills[skill].expectLet {
                ret[ActionIdentifier.Skill(item, skill)] = it.action
            }
        }
    }

    return ret
}

fun GameWorld.activeAction(ent: Entity): Action? {
    val cd = ent[CharacterData] ?: return null
    val ident = cd.activeAction ?: return null
    return possibleActions(ent)[ident]
}


fun GameWorld.performAction(ent: Entity, action: Action, chosenTargets: List<EffectTarget>) {
    val cd = ent[CharacterData] ?: return Noto.err("Expected character data for $ent")

    if (action.effects.size != chosenTargets.size) {
        Noto.err("performAction(...) called with incorrect target count, ${ent.toString(this)}, $action, $chosenTargets")
        return
    }

    if (useAP(ent, action.ap.toDouble())) {
        for (i in 0 until action.effects.size) {
            val effect = action.effects[i]
            val target = chosenTargets[i]
            if (!applyEffect(ent, effect, target)) {
                Noto.info("effect $effect could not be applied, not applying any further effects")
                break
            }
        }
        fireEvent(CharacterPerformedAction(ent, action, chosenTargets))
    } else {
        Noto.info("Insufficient ap to perform action ${ent.toString(this)}, $action, $chosenTargets")
    }
}

fun GameWorld.applyEffect(ent: Entity, effect: TargetedEffect, target: EffectTarget) : Boolean {
    val eff = effect.effect

    fun strInfo() : String {
        return "${prettyString(ent)}, $effect, $target"
    }

    return when(eff) {
        is Effect.AttackEffect -> {
            when (target) {
                is EffectTarget.Entity -> {
                    val cd = target.entity[CharacterData] ?: return Noto.errAndReturn("target of attack was not a character ${strInfo()}", false)
                    val hpLostBefore = cd.hpLost
                    cd.hpLost += eff.attack.damage
                    Noto.info("Character ${prettyString(target.entity)} attacked, dealt ${eff.attack.damage} damage, new hp ${effectiveCombatStats(target.entity).maxHP - cd.hpLost}")
                    fireEvent(DamageTaken(target.entity, hpLostBefore, cd.hpLost))
                    fireEvent(CharacterAttacked(ent, target.entity))

                    if (cd.hpLost >= effectiveCombatStats(target.entity).maxHP) {
                        cd.dead = true
                        val pd = target.entity[Physical] ?: return Noto.errAndReturn("target of attack died, but was not a physical entity ${strInfo()}", false)
                        val tm = global(TacticalMap) ?: return Noto.errAndReturn("somehow there's no tactical map ${strInfo()}", false)
                        val tile = tm.tiles[pd.position.xy]
                        tile.entities = tile.entities - target.entity
                        fireEvent(CharacterDied(target.entity))
                    }
                    true
                }
                else -> {
                    Noto.errAndReturn("Invalid target type for attack effect ${strInfo()}", false)
                }
            }
        }
        Effect.Move -> {
            val pd = ent[Physical] ?: return Noto.errAndReturn("Somehow attempted to move a non-physical entity ${prettyString(ent)}", false)
            when (target) {
                is EffectTarget.Path -> {
                    moveCharacter(ent, target.path)
                }
                else -> Noto.errAndReturn("Invalid target for move effect: ${strInfo()}", false)
            }
        }
        Effect.NoOp -> {
            Noto.warn("applying NoOp effect, which is somewhat surprising ${strInfo()}")
            true
        }
        is Effect.StatsEffect -> {
            when (target) {
                is EffectTarget.Entity -> {
                    target.entity[CharacterData].expectLet { cd -> cd.statsChanges = cd.statsChanges + StatsChange(stats = eff.stats, endCondition = eff.duration) }
                    true
                }
                else -> {
                    Noto.err("Invalid target for stats effect: ${strInfo()}")
                    false
                }
            }
        }
    }
}


fun GameWorld.distanceBetween(a: Entity, b: Entity) : Float? {
    val apd = a[Physical] ?: return null
    val bpd = b[Physical] ?: return null
    return apd.position.distanceTo(bpd.position)
}